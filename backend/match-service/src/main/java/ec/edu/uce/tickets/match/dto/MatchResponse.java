package ec.edu.uce.tickets.match.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ec.edu.uce.tickets.match.entity.MatchStatus;
import ec.edu.uce.tickets.match.entity.SportMatch;

import java.time.LocalDate;
import java.time.LocalTime;

public record MatchResponse(
        Long id,
        String name,
        String description,
        LocalDate matchDate,
        @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        MatchStatus status,
        StadiumSummary stadium
) {
    public record StadiumSummary(Long id, String name) {
    }

    public static MatchResponse from(SportMatch match) {
        return new MatchResponse(
                match.getId(),
                match.getName(),
                match.getDescription(),
                match.getMatchDate(),
                match.getMatchTime(),
                match.getStatus(),
                new StadiumSummary(match.getStadium().getId(), match.getStadium().getName())
        );
    }
}