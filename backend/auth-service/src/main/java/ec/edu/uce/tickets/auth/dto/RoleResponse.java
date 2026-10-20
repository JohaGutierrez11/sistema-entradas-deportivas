package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.entity.Role;
import ec.edu.uce.tickets.auth.entity.RoleStatus;

public record RoleResponse(Long id, String name, String description, RoleStatus status) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.getStatus());
    }
}