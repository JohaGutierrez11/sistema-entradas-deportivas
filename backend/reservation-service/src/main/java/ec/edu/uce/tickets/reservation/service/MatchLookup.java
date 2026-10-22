package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Datos del partido para mostrar (historial, entradas). Si match-service no responde, devuelve null. */
@Slf4j
@Component
public class MatchLookup {

    private final MatchServiceClient matchServiceClient;

    public MatchLookup(MatchServiceClient matchServiceClient) {
        this.matchServiceClient = matchServiceClient;
    }

    /** Degradación elegante: mostrar sin datos del partido es mejor que no mostrar nada. */
    public MatchSummary findOrNull(Long matchId) {
        try {
            return MatchSummary.from(matchServiceClient.getMatch(matchId));
        } catch (ServiceUnavailableException | ResourceNotFoundException e) {
            log.warn("Sin datos del partido {}: {}", matchId, e.getMessage());
            return null;
        }
    }
}