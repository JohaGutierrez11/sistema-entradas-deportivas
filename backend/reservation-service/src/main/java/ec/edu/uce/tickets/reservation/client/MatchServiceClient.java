package ec.edu.uce.tickets.reservation.client;

import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;

@Slf4j
@Component
public class MatchServiceClient {

    private static final String UNAVAILABLE =
            "El servicio de partidos no está disponible en este momento. Intente más tarde.";

    private final RestClient restClient;

    public MatchServiceClient(RestClient matchServiceRestClient) {
        this.restClient = matchServiceRestClient;
    }

    /** Consulta un partido. 404 si no existe; 503 si match-service no responde. */
    public MatchInfo getMatch(Long matchId) {
        try {
            return restClient.get()
                    .uri("/match-api/v1/matches/{id}", matchId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("No existe el partido con el id " + matchId);
                    })
                    .body(MatchInfo.class);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Fallo al consultar el partido {} en match-service: {}", matchId, ex.getMessage());
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
    }

    /** Pregunta qué asientos existen y son del estadio del partido (endpoint interno). */
    public SeatValidationResult validateSeats(Long matchId, Collection<Long> seatIds) {
        try {
            return restClient.post()
                    .uri("/match-api/v1/internal/seats/validate")
                    .body(new SeatValidationRequest(matchId, seatIds))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("No existe el partido con el id " + matchId);
                    })
                    .body(SeatValidationResult.class);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Fallo al validar asientos del partido {} en match-service: {}", matchId, ex.getMessage());
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
    }

    /** Ocupación por sección de un partido, a partir de los asientos vendidos (endpoint interno). */
    public MatchOccupancy getOccupancy(Long matchId, Collection<Long> soldSeatIds) {
        try {
            return restClient.post()
                    .uri("/match-api/v1/internal/matches/{id}/occupancy", matchId)
                    .body(new OccupancyRequest(soldSeatIds))
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResourceNotFoundException("No existe el partido con el id " + matchId);
                    })
                    .body(MatchOccupancy.class);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Fallo al consultar la ocupación del partido {} en match-service: {}", matchId, ex.getMessage());
            throw new ServiceUnavailableException(UNAVAILABLE);
        }
    }
}