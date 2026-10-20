package ec.edu.uce.tickets.auth.service;

import ec.edu.uce.tickets.auth.dto.*;
import ec.edu.uce.tickets.auth.entity.*;
import ec.edu.uce.tickets.auth.exception.BusinessRuleException;
import ec.edu.uce.tickets.auth.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.auth.repository.AppUserRepository;
import ec.edu.uce.tickets.auth.repository.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;

    public UserService(AppUserRepository appUserRepository, RoleRepository roleRepository) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        return appUserRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String adminUsername) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con el id " + id));

        // BLOQUEADO es del sistema, no del administrador
        if (request.status() == UserStatus.BLOQUEADO) {
            throw new BusinessRuleException(
                    "El estado BLOQUEADO lo asigna el sistema por intentos fallidos; no se asigna manualmente");
        }
        // Un admin no puede dejarse a sí mismo fuera
        if (user.getUsername().equalsIgnoreCase(adminUsername) && request.status() == UserStatus.INACTIVO) {
            throw new BusinessRuleException("Un administrador no puede desactivar su propia cuenta");
        }

        user.setStatus(request.status());
        if (request.status() == UserStatus.ACTIVO) {   // activar también sirve para desbloquear
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAll(Sort.by("id")).stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public RoleResponse updateRoleStatus(Long id, UpdateRoleStatusRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el rol con el id " + id));

        // Desactivar ADMIN dejaría el sistema sin nadie que lo administre
        if ("ADMIN".equals(role.getName()) && request.status() == RoleStatus.INACTIVO) {
            throw new BusinessRuleException("No se puede desactivar el rol ADMIN");
        }

        role.setStatus(request.status());
        return RoleResponse.from(role);
    }
}