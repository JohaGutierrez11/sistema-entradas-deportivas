package ec.edu.uce.tickets.reservation.service;

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

@Service
public class ReservationWriter {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final ParameterService parameterService;
    private final SecureRandom random = new SecureRandom();

    public ReservationWriter(ReservationRepository reservationRepository, SeatLockRepository seatLockRepository,
                             ParameterService parameterService) {
        this.reservationRepository = reservationRepository;
        this.seatLockRepository = seatLockRepository;
        this.parameterService = parameterService;
    }

    @Transactional
    public Reservation createPending(Long clientId, Long matchId, Set<Long> seatIds) {
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
        reservation.setExpiresAt(now.plusMinutes(parameterService.getInt("HOLD_MINUTES")));

        for (Long seatId : seatIds) {
            SeatLock lock = new SeatLock();
            lock.setMatchId(matchId);
            lock.setSeatId(seatId);
            lock.setStatus(SeatLockStatus.HELD);
            reservation.addSeatLock(lock);
        }

        // saveAndFlush: si dos clientes chocan en el mismo asiento, el UNIQUE falla AQUÍ → 409
        return reservationRepository.saveAndFlush(reservation);
    }

    /** Marca como EXPIRADAS las reservas pendientes vencidas de un partido y libera sus asientos. */
    @Transactional
    public void expirePending(Long matchId, LocalDateTime now) {
        List<Reservation> expired = reservationRepository
                .findByMatchIdAndStatusAndExpiresAtLessThanEqual(matchId, ReservationStatus.PENDIENTE, now);
        for (Reservation reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRADA);
            reservation.getSeatLocks().clear();   // orphanRemoval → DELETE de sus seat_lock
        }
        // Hibernate hace INSERT antes que DELETE: forzamos los DELETE ahora
        reservationRepository.flush();
    }

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

    @Transactional
    public Reservation confirm(UUID publicId, Long clientId) {
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

        return reservationRepository.saveAndFlush(reservation);
    }
}