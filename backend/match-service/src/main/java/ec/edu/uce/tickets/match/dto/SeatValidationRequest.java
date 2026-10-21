package ec.edu.uce.tickets.match.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record SeatValidationRequest(
        @NotNull @Min(1) Long matchId,
        @NotEmpty @Size(max = 6) Set<@NotNull @Min(1) Long> seatIds
) {
}