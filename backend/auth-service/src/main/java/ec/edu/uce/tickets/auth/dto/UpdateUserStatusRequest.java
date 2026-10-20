package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}