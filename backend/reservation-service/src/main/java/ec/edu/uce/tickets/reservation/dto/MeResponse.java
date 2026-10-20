package ec.edu.uce.tickets.reservation.dto;

import java.time.Instant;

public record MeResponse(String username, String role, Long clientId, Instant expiresAt) {
}