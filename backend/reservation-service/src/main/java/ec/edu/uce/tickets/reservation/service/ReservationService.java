package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.client.MatchInfo;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.client.SeatValidationResult;
import ec.edu.uce.tickets.reservation.dto.AdminReservationItem;
import ec.edu.uce.tickets.reservation.dto.CreateReservationRequest;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;
import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.ReservationRepository;
import ec.edu.uce.tickets.reservation.repository.SeatLockRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final MatchServiceClient matchServiceClient;
    private final ReservationWriter reservationWriter;
    private final ReservationRepository reservationRepository;
    private final SeatLockRepository seatLockRepository;
    private final AuditService auditService;
    private final SalesWindow salesWindow;
    private final ParameterService parameterService;

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReservationService(MatchServiceClient matchServiceClient, ReservationWriter reservationWriter,
                              ReservationRepository reservationRepository, SeatLockRepository seatLockRepository, AuditService auditService,
                              SalesWindow salesWindow, ParameterService parameterService) {
        this.matchServiceClient = matchServiceClient;
        this.reservationWriter = reservationWriter;
        this.reservationRepository = reservationRepository;
        this.seatLockRepository = seatLockRepository;
        this.auditService = auditService;
        this.salesWindow = salesWindow;
        this.parameterService = parameterService;
    }

    // SIN @Transactional: primero se consulta a match-service y después se abre la transacción
    public ReservationResponse create(Long clientId, CreateReservationRequest request) {
        // 3. Validar partido y asientos con match-service (404 / 503)
        SeatValidationResult validation = matchServiceClient.validateSeats(request.matchId(), request.seatIds());

        // ¿Sigue abierta la venta? Por estado Y por hora (no depende de que el admin presione "Iniciar")
        LocalDateTime salesCloseAt = salesWindow.assertOpen(
                validation.matchStatus(), validation.matchDate(), validation.matchTime(), "reservas");
        if (!validation.invalidSeatIds().isEmpty()) {
            throw new ResourceNotFoundException("Estos asientos no existen en el estadio del partido: "
                    + validation.invalidSeatIds());
        }

        ReservationResponse created = reservationWriter.createPending(clientId, request.matchId(), request.seatIds(), salesCloseAt);
        auditService.success(AuditAction.RESERVA_CREADA, "RESERVA", created.code(),
                "Partido " + created.matchId() + ", asientos " + created.seatIds());
        return created;
    }

    @Transactional(readOnly = true)
    public ReservationResponse findOne(UUID publicId, Long clientId, boolean admin) {
        Reservation reservation = reservationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva " + publicId));

        // IDOR: un cliente solo ve SUS reservas. Responde 404 (no 403) para no revelar que existe
        if (!admin && !reservation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("No existe la reserva " + publicId);
        }
        return ReservationResponse.from(reservation);
    }

    // SIN @Transactional: primero match-service, después la transacción
    public ReservationResponse confirm(UUID publicId, Long clientId) {
        Reservation reservation = reservationRepository.findByPublicId(publicId)
                .filter(r -> r.getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva " + publicId));

        // ¿Cancelaron el partido o cerró la venta mientras el cliente decidía?
        MatchInfo match = matchServiceClient.getMatch(reservation.getMatchId());
        salesWindow.assertOpen(match.status(), match.matchDate(), match.matchTime(), "confirmaciones");

        ReservationResponse confirmed = reservationWriter.confirm(publicId, clientId);
        auditService.success(AuditAction.RESERVA_CONFIRMADA, "RESERVA", confirmed.code(),
                confirmed.tickets().size() + " tickets emitidos");
        return confirmed;
    }

    /** La reserva si existe y quien pregunta puede verla (dueño o admin). Si no, 404 (anti-IDOR). */
    private Reservation findAccessible(UUID publicId, Long clientId, boolean admin) {
        return reservationRepository.findByPublicId(publicId)
                .filter(r -> admin || r.getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la reserva " + publicId));
    }

    // SIN @Transactional: si hace falta, primero match-service; después la transacción
    public ReservationResponse cancel(UUID publicId, Long clientId, boolean admin) {
        Reservation reservation = findAccessible(publicId, clientId, admin);

        // Solo una reserva pagada depende del partido: no se devuelve una entrada de un partido jugado
        if (reservation.getStatus() == ReservationStatus.CONFIRMADA) {
            MatchInfo match = matchServiceClient.getMatch(reservation.getMatchId());
            if ("EN_CURSO".equals(match.status()) || "FINALIZADO".equals(match.status())) {
                throw new BusinessRuleException("No se puede cancelar porque el partido está " + match.status());
            }
            // HU-45: el cliente cancela una reserva pagada solo hasta CANCEL_DEADLINE_HOURS antes del inicio.
            // El admin no tiene ese límite (atiende casos excepcionales) y un partido CANCELADO siempre se puede.
            if (!admin && "PROGRAMADO".equals(match.status())) {
                int hours = parameterService.getInt("CANCEL_DEADLINE_HOURS");
                LocalDateTime deadline = LocalDateTime.of(match.matchDate(), match.matchTime()).minusHours(hours);
                if (!LocalDateTime.now(ECUADOR).isBefore(deadline)) {
                    throw new BusinessRuleException("Las reservas confirmadas se pueden cancelar hasta " + hours
                            + " horas antes del inicio del partido (el plazo venció el " + deadline.format(DEADLINE_FORMAT) + ")");
                }
            }
        }

        ReservationResponse cancelled = reservationWriter.cancel(publicId, clientId, admin);
        auditService.success(AuditAction.RESERVA_CANCELADA, "RESERVA", cancelled.code(),
                admin ? "Cancelada por un administrador" : "Cancelada por el cliente");
        return cancelled;
    }

    /** Todas las reservas de un cliente, de la más nueva a la más antigua. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findByClient(Long clientId) {
        return reservationRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    /** Reservas filtradas para el reporte. Todos los filtros son opcionales. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> search(LocalDate from, LocalDate to, Long matchId, ReservationStatus status) {
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (from != null) {
                filters.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                // "hasta el 31" incluye el 31 completo: menor que el 1 a las 00:00
                filters.add(cb.lessThan(root.<LocalDateTime>get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            if (matchId != null) {
                filters.add(cb.equal(root.get("matchId"), matchId));
            }
            if (status != null) {
                filters.add(cb.equal(root.get("status"), status));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };
        return reservationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    /** Reservas de un partido para el admin, con el cliente al que pertenecen (HU-28). */
    @Transactional(readOnly = true)
    public List<AdminReservationItem> searchForAdmin(Long matchId, ReservationStatus status) {
        Specification<Reservation> spec = (root, query, cb) -> status == null
                ? cb.equal(root.get("matchId"), matchId)
                : cb.and(cb.equal(root.get("matchId"), matchId), cb.equal(root.get("status"), status));
        return reservationRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(r -> new AdminReservationItem(r.getClientId(), ReservationResponse.from(r)))
                .toList();
    }

    /** Asientos vendidos de un partido. */
    @Transactional(readOnly = true)
    public List<Long> findSoldSeatIds(Long matchId) {
        return seatLockRepository.findSeatIds(matchId, SeatLockStatus.RESERVED);
    }
}