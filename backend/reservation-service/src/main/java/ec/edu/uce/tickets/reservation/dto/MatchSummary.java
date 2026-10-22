package ec.edu.uce.tickets.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ec.edu.uce.tickets.reservation.client.MatchInfo;

import java.time.LocalDate;
import java.time.LocalTime;

/** Lo que el historial muestra de un partido. */
public record MatchSummary(
        String name,
        LocalDate matchDate,
        @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        String stadiumName
) {
    public static MatchSummary from(MatchInfo match) {
        return new MatchSummary(match.name(), match.matchDate(), match.matchTime(),
                match.stadium() != null ? match.stadium().name() : null);
    }
}