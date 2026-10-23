package ec.edu.uce.tickets.reservation.client;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SeatValidationResult(
        Long matchId,
        String matchStatus,
        LocalDate matchDate,
        @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        Long stadiumId,
        List<Long> validSeatIds,
        List<Long> invalidSeatIds
) {
}