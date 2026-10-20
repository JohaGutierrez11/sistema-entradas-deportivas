package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.entity.AppUser;
import ec.edu.uce.tickets.auth.entity.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String role,
        UserStatus status,
        boolean mustChangePassword,
        int failedLoginAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().getName(),
                user.getStatus(), user.isMustChangePassword(), user.getFailedLoginAttempts(),
                user.getLockedUntil(), user.getLastLoginAt());
    }
}