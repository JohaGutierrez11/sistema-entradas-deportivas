package ec.edu.uce.tickets.auth.controller;

import ec.edu.uce.tickets.auth.dto.*;
import ec.edu.uce.tickets.auth.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Administración de usuarios y roles (ADMIN)")
@RestController
@RequestMapping("/auth-api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserResponse> findAllUsers() {
        return userService.findAllUsers();
    }

    @PatchMapping("/users/{userId}/status")
    public UserResponse updateUserStatus(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateUserStatusRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return userService.updateUserStatus(userId, request, jwt.getSubject());
    }

    @GetMapping("/roles")
    public List<RoleResponse> findAllRoles() {
        return userService.findAllRoles();
    }

    @PatchMapping("/roles/{roleId}/status")
    public RoleResponse updateRoleStatus(@PathVariable Long roleId,
                                         @Valid @RequestBody UpdateRoleStatusRequest request) {
        return userService.updateRoleStatus(roleId, request);
    }
}