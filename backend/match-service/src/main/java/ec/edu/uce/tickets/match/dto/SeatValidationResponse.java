package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.MatchStatus;

import java.util.List;

public record SeatValidationResponse(
        Long matchId,
        MatchStatus matchStatus,
        Long stadiumId,
        List<Long> validSeatIds,
        List<Long> invalidSeatIds
) {
}