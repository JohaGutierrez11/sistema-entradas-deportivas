package ec.edu.uce.tickets.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID publicId,
        String username,
        String email,
        String message
) {
}