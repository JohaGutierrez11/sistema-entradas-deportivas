package ec.edu.uce.tickets.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        String role,
        boolean mustChangePassword
) {
}