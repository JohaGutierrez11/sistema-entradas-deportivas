package ec.edu.uce.tickets.match.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ec.edu.uce.tickets.match.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.List;

public record SeatValidationResponse(
        Long matchId,
        MatchStatus matchStatus,
        LocalDate matchDate,
        @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        Long stadiumId,
        List<Long> validSeatIds,
        List<Long> invalidSeatIds
) {
}