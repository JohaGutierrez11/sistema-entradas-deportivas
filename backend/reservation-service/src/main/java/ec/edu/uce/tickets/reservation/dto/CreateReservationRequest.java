package ec.edu.uce.tickets.reservation.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record CreateReservationRequest(
        @NotNull @Min(1) Long matchId,
        @NotEmpty @Size(max = 6) Set<@NotNull @Min(1) Long> seatIds
) {
}