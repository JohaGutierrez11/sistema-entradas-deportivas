package ec.edu.uce.tickets.reservation.client;

import java.util.List;

public record SeatValidationResult(
        Long matchId,
        String matchStatus,
        Long stadiumId,
        List<Long> validSeatIds,
        List<Long> invalidSeatIds
) {
}