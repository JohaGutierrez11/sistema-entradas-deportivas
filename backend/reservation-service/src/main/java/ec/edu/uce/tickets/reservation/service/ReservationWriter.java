package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.entity.*;
import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import ec.edu.uce.tickets.reservation.exception.ResourceInUseException;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.ReservationRepository;
import ec.edu.uce.tickets.reservation.repository.SeatLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Operaciones de escritura sobre reservas, cada una en su propia transacción.
 * Regla: los métodos públicos devuelven DTOs (no entidades), construidos con la sesión abierta.
 */
@Service
public class ReservationWriter {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ParameterService parameterService;
    private final QrCodeGenerator qrCodeGenerator;
    private final SecureRandom random = new SecureRandom();

    public ReservationWriter(ReservationRepository reservationRepository, SeatLockRepository seatLockRepository,
                             ParameterService parameterService, QrCodeGenerator qrCodeGenerator) {
        this.reservationRepository = reservationRepository;
        this.seatLockRepository = seatLockRepository;
        this.parameterService = parameterService;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    // ------------------------------------------------------------------ HU-16: reservar

    @Transactional
    public ReservationResponse createPending(Long clientId, Long matchId, Set<Long> seatIds, LocalDateTime salesCloseAt) {
        LocalDateTime now = LocalDateTime.now(ECUADOR);

        // 4. Liberar reservas vencidas de este partido (por si la tarea automática aún no pasó)
        expirePending(matchId, now);

        // 5. Máximo de entradas por cliente y partido (leído de la tabla parameter)
        int max = parameterService.getInt("MAX_TICKETS_PER_MATCH");
        long current = seatLockRepository.countActiveSeatsOfClient(clientId, matchId, SeatLockStatus.RESERVED, now);
        if (current + seatIds.size() > max) {
            throw new BusinessRuleException("Solo puedes tener " + max + " entradas por partido. Ya tienes "
                    + current + " y estás pidiendo " + seatIds.size());
        }

        // 6. Asientos ocupados: capa 1 (mensaje claro). La capa 2 es el UNIQUE de la base
        List<Long> occupied = seatLockRepository.findOccupiedSeatIds(matchId, seatIds, SeatLockStatus.RESERVED, now);
        if (!occupied.isEmpty()) {
            throw new ResourceInUseException("Estos asientos ya no están disponibles: "
                    + occupied.stream().sorted().toList());
        }

        // 7. Crear la reserva PENDIENTE con sus asientos retenidos
        Reservation reservation = new Reservation();
        reservation.setCode(generateCode(now));
        reservation.setClientId(clientId);
        reservation.setMatchId(matchId);
        reservation.setStatus(ReservationStatus.PENDIENTE);
        reservation.setCreatedAt(now);
        // Vence a los HOLD_MINUTES, pero nunca después del cierre de la venta: no se puede confirmar después
        LocalDateTime holdUntil = now.plusMinutes(parameterService.getInt("HOLD_MINUTES"));
        reservation.setExpiresAt(holdUntil.isBefore(salesCloseAt) ? holdUntil : salesCloseAt);

        for (Long seatId : seatIds) {
            SeatLock lock = new SeatLock();
            lock.setMatchId(matchId);
            lock.setSeatId(seatId);
            lock.setStatus(SeatLockStatus.HELD);
            reservation.addSeatLock(lock);
        }

        // saveAndFlush: si dos clientes chocan en el mismo asiento, el UNIQUE falla AQUÍ → 409
        return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
    }

    // ------------------------------------------------------------------ HU-18: confirmar

    @Transactional
    public ReservationResponse confirm(UUID publicId, Long clientId) {
        // Bloqueo: si llegan dos "confirmar" a la vez, la segunda espera aquí
        Reservation reservation = reservationRepository.findByPublicIdForUpdate(publicId)
                .filter(r -> r.getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva " + publicId));

        LocalDateTime now = LocalDateTime.now(ECUADOR);

        if (reservation.getStatus() != ReservationStatus.PENDIENTE) {
            throw new ResourceInUseException("La reserva está " + reservation.getStatus() + " y no se puede confirmar");
        }
        if (!reservation.getExpiresAt().isAfter(now)) {
            throw new ResourceInUseException("La reserva venció. Vuelve a elegir tus asientos");
        }

        reservation.setStatus(ReservationStatus.CONFIRMADA);
        reservation.setConfirmedAt(now);

        for (SeatLock lock : reservation.getSeatLocks()) {
            lock.setStatus(SeatLockStatus.RESERVED);          // de retenido a vendido

            Ticket ticket = new Ticket();
            ticket.setSeatId(lock.getSeatId());
            ticket.setQrCode(qrCodeGenerator.newCode());
            ticket.setStatus(TicketStatus.ACTIVO);
            reservation.addTicket(ticket);
        }

        // La respuesta se arma AQUÍ, con la sesión abierta (evita LazyInitializationException)
        return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
    }

    // ------------------------------------------------------------------ HU-17: expiración

    /** Lo llama la tarea programada (desde OTRA clase, así que @Transactional sí aplica). */
    @Transactional
    public List<String> expireAllPending() {
        LocalDateTime now = LocalDateTime.now(ECUADOR);
        List<Reservation> expired = reservationRepository
                .findExpiredForUpdate(ReservationStatus.PENDIENTE, now);

        for (Reservation reservation : expired) {
            reservation.expire();
        }
        // No hace falta save: dirty checking guarda los cambios al terminar la transacción
        return expired.stream().map(Reservation::getCode).toList();
    }

    /** Corre dentro de la transacción de quien la llama (createPending). */
    private void expirePending(Long matchId, LocalDateTime now) {
        List<Reservation> expired = reservationRepository
                .findByMatchIdAndStatusAndExpiresAtLessThanEqual(matchId, ReservationStatus.PENDIENTE, now);
        for (Reservation reservation : expired) {
            reservation.expire();
        }
        // Hibernate hace INSERT antes que DELETE: forzamos los DELETE ahora
        reservationRepository.flush();
    }

    // ------------------------------------------------------------------ utilidades

    /** "RES-2026-K7P4QX": legible para personas, aleatorio y no secuencial. */
    private String generateCode(LocalDateTime now) {
        String prefix = parameterService.getString("RESERVATION_CODE_PREFIX");
        String code;
        do {
            StringBuilder random6 = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                random6.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = prefix + "-" + now.getYear() + "-" + random6;
        } while (reservationRepository.existsByCode(code));
        return code;
    }

    // ------------------------------------------------------------------ HU-19: cancelar

    /**
     * HU-46: el partido se canceló. Todas sus reservas activas pasan a CANCELADA, sus asientos se liberan
     * y sus entradas quedan ANULADAS (mismo cambio de estado que una cancelación normal).
     * Devuelve los códigos para la auditoría.
     */
    @Transactional
    public List<String> cancelAllOfMatch(Long matchId) {
        LocalDateTime now = LocalDateTime.now(ECUADOR);
        List<Reservation> active = reservationRepository.findByMatchIdAndStatusInForUpdate(matchId,
                List.of(ReservationStatus.PENDIENTE, ReservationStatus.CONFIRMADA));
        active.forEach(reservation -> reservation.cancel(now));
        return active.stream().map(Reservation::getCode).toList();
    }

    @Transactional
    public ReservationResponse cancel(UUID publicId, Long clientId, boolean admin) {
        // Mismo bloqueo que confirmar: cancelar, confirmar y expirar nunca se pisan
        Reservation reservation = reservationRepository.findByPublicIdForUpdate(publicId)
                .filter(r -> admin || r.getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva " + publicId));

        LocalDateTime now = LocalDateTime.now(ECUADOR);
        ReservationStatus status = reservation.getStatus();

        if (status == ReservationStatus.CANCELADA || status == ReservationStatus.EXPIRADA) {
            throw new ResourceInUseException("La reserva ya está " + status);
        }
        if (status == ReservationStatus.PENDIENTE && !reservation.getExpiresAt().isAfter(now)) {
            throw new ResourceInUseException("La reserva ya venció; sus asientos se liberan solos");
        }
        if (reservation.hasUsedTickets()) {
            throw new ResourceInUseException("No se puede cancelar porque al menos una entrada ya fue utilizada");
        }

        reservation.cancel(now);
        return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
    }
}