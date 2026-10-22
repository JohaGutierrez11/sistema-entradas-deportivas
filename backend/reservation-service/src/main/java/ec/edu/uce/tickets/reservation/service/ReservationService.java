package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.client.MatchInfo;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.client.SeatValidationResult;
import ec.edu.uce.tickets.reservation.dto.CreateReservationRequest;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReservationService {

    private final MatchServiceClient matchServiceClient;
    private final ReservationWriter reservationWriter;
    private final ReservationRepository reservationRepository;

    public ReservationService(MatchServiceClient matchServiceClient, ReservationWriter reservationWriter,
                              ReservationRepository reservationRepository) {
        this.matchServiceClient = matchServiceClient;
        this.reservationWriter = reservationWriter;
        this.reservationRepository = reservationRepository;
    }

    // SIN @Transactional: primero se consulta a match-service y después se abre la transacción
    public ReservationResponse create(Long clientId, CreateReservationRequest request) {
        // 3. Validar partido y asientos con match-service (404 / 503)
        SeatValidationResult validation = matchServiceClient.validateSeats(request.matchId(), request.seatIds());

        if (!"PROGRAMADO".equals(validation.matchStatus())) {
            throw new BusinessRuleException("El partido está " + validation.matchStatus() + " y no admite reservas");
        }
        if (!validation.invalidSeatIds().isEmpty()) {
            throw new ResourceNotFoundException("Estos asientos no existen en el estadio del partido: "
                    + validation.invalidSeatIds());
        }

        // 4 a 7. Reglas locales y creación, dentro de la transacción (en OTRA clase)
        return reservationWriter.createPending(clientId, request.matchId(), request.seatIds());
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

        // ¿Cancelaron el partido mientras el cliente decidía?
        MatchInfo match = matchServiceClient.getMatch(reservation.getMatchId());
        if (!"PROGRAMADO".equals(match.status())) {
            throw new BusinessRuleException("El partido está " + match.status() + " y ya no admite confirmaciones");
        }

        return reservationWriter.confirm(publicId, clientId);
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
        }

        return reservationWriter.cancel(publicId, clientId, admin);
    }
}