package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.entity.RoleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleStatusRequest(@NotNull RoleStatus status) {
}