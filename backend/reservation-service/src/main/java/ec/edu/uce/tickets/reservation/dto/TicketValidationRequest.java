package ec.edu.uce.tickets.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo que llega desde la puerta: el texto del QR leído y el partido que se está controlando (HU-47). */
public record TicketValidationRequest(
        @NotBlank @Size(max = 64) String qrCode,
        @NotNull @Min(1) Long matchId
) {
}
