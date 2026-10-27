package ec.edu.uce.tickets.reservation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta del control de acceso (HU-47). Siempre 200: la verificación se hizo; "valid" dice si la
 * persona entra o no, y "result" el motivo (para que la pantalla de la puerta muestre verde o rojo).
 */
public record TicketValidationResponse(
        boolean valid,
        Result result,
        String message,
        UUID ticketPublicId,
        Long seatId,
        String reservationCode,
        Long clientId,
        LocalDateTime usedAt
) {
    public enum Result { VALIDA, YA_UTILIZADA, ANULADA, OTRO_PARTIDO, NO_RECONOCIDA, PARTIDO_NO_HABILITADO }

    public static TicketValidationResponse rejected(Result result, String message) {
        return new TicketValidationResponse(false, result, message, null, null, null, null, null);
    }
}
