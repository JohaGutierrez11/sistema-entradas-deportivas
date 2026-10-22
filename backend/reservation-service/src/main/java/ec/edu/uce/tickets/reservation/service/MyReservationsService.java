package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.dto.MyReservationItem;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.exception.ServiceUnavailableException;
import ec.edu.uce.tickets.reservation.report.MyReservationsPdf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** "Mis reservas" (HU-20): el historial del cliente en JSON o en PDF. SIN transacción. */
@Slf4j
@Service
public class MyReservationsService {

    private final ReservationService reservationService;
    private final MatchServiceClient matchServiceClient;
    private final MyReservationsPdf myReservationsPdf;

    public MyReservationsService(ReservationService reservationService, MatchServiceClient matchServiceClient,
                                 MyReservationsPdf myReservationsPdf) {
        this.reservationService = reservationService;
        this.matchServiceClient = matchServiceClient;
        this.myReservationsPdf = myReservationsPdf;
    }

    public List<MyReservationItem> findMine(Long clientId) {
        // (a) Nuestra base, con transacción (en OTRA clase)
        List<ReservationResponse> reservations = reservationService.findByClient(clientId);

        // (b) match-service, fuera de la transacción: una sola llamada por partido distinto
        Map<Long, MatchSummary> matches = new HashMap<>();
        for (Long matchId : reservations.stream().map(ReservationResponse::matchId).distinct().toList()) {
            matches.put(matchId, findMatchOrNull(matchId));
        }

        return reservations.stream()
                .map(reservation -> new MyReservationItem(reservation, matches.get(reservation.matchId())))
                .toList();
    }

    public byte[] findMineAsPdf(Long clientId, String username) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Guayaquil"));
        return myReservationsPdf.build(username, findMine(clientId), now);
    }

    /** Degradación elegante: sin match-service, el historial se muestra igual, sin datos del partido. */
    private MatchSummary findMatchOrNull(Long matchId) {
        try {
            return MatchSummary.from(matchServiceClient.getMatch(matchId));
        } catch (ServiceUnavailableException | ResourceNotFoundException e) {
            log.warn("Sin datos del partido {} para el historial: {}", matchId, e.getMessage());
            return null;
        }
    }
}