package ec.edu.uce.tickets.reservation.client;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/** Lo que reservas necesita saber de un partido. */
public record MatchInfo(
        Long id,
        String name,
        LocalDate matchDate,
        @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        String status,
        StadiumInfo stadium
) {
    public record StadiumInfo(Long id, String name) {
    }
}