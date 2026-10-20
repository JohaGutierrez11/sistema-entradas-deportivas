package ec.edu.uce.tickets.auth.service;

import ec.edu.uce.tickets.auth.dto.*;
import ec.edu.uce.tickets.auth.entity.*;
import ec.edu.uce.tickets.auth.exception.*;
import ec.edu.uce.tickets.auth.repository.AppUserRepository;
import ec.edu.uce.tickets.auth.repository.ClientRepository;
import ec.edu.uce.tickets.auth.repository.RoleRepository;
import ec.edu.uce.tickets.auth.security.CredentialGenerator;
import ec.edu.uce.tickets.auth.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AuthService {

    private static final String CLIENT_ROLE = "CLIENTE";

    private final AppUserRepository appUserRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;
    private final CredentialsMailService mailService;
    private final TokenService tokenService;
    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    public AuthService(AppUserRepository appUserRepository, ClientRepository clientRepository,
                       RoleRepository roleRepository, CredentialGenerator credentialGenerator,
                       PasswordEncoder passwordEncoder, CredentialsMailService mailService, TokenService tokenService) {
        this.appUserRepository = appUserRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.credentialGenerator = credentialGenerator;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.tokenService = tokenService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String cedula = request.cedula().trim();
        String email = request.email().trim().toLowerCase();

        // 1. Cédula y correo únicos → 409
        if (clientRepository.existsByCedula(cedula)) {
            throw new DuplicateResourceException("Ya existe un cliente registrado con esa cédula");
        }
        if (clientRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Ya existe un cliente registrado con ese correo");
        }

        // 2. El rol CLIENTE debe existir y estar activo
        Role clientRole = roleRepository.findByName(CLIENT_ROLE)
                .filter(role -> role.getStatus() == RoleStatus.ACTIVO)
                .orElseThrow(() -> new IllegalStateException("El rol CLIENTE no existe o está inactivo"));

        // 3. Generar credenciales
        String username = credentialGenerator.generateUsername(request.firstName(), request.lastName());
        String temporaryPassword = credentialGenerator.generateTemporaryPassword();

        // 4. Crear el usuario: la contraseña se guarda SOLO como hash BCrypt
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole(clientRole);
        user.setStatus(UserStatus.ACTIVO);
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        appUserRepository.save(user);

        // 5. Crear el cliente asociado al usuario
        Client client = new Client();
        client.setFirstName(request.firstName().trim());
        client.setMiddleName(request.middleName() == null ? null : request.middleName().trim());
        client.setLastName(request.lastName().trim());
        client.setCedula(cedula);
        client.setNationality(request.nationality().trim());
        client.setAddress(request.address().trim());
        client.setPhone(request.phone().trim());
        client.setEmail(email);
        client.setUser(user);
        clientRepository.save(client);

        // 6. Enviar las credenciales. Si el correo falla, se deshace todo (rollback)
        mailService.sendCredentials(email, client.getFirstName() + " " + client.getLastName(),
                username, temporaryPassword);

        return new RegisterResponse(client.getPublicId(), username, email,
                "Te enviamos tus credenciales de acceso al correo " + email);
    }

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now(ECUADOR);

        // 1. Si el usuario no existe: el MISMO mensaje que si la contraseña es incorrecta
        AppUser user = appUserRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Bloqueo temporal: si ya pasaron los 15 minutos, se desbloquea solo
        if (user.getStatus() == UserStatus.BLOQUEADO) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                throw new AccountLockedException(user.getLockedUntil());              // 423
            }
            user.setStatus(UserStatus.ACTIVO);
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        // 3. Contraseña incorrecta: suma un intento; al quinto, se bloquea
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(UserStatus.BLOQUEADO);
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            }
            throw new InvalidCredentialsException();                                  // 401
        }

        // 4. Contraseña correcta, pero la cuenta o el rol están inactivos
        if (user.getStatus() == UserStatus.INACTIVO || user.getRole().getStatus() == RoleStatus.INACTIVO) {
            throw new AccountDisabledException();                                     // 403
        }

        // 5. Éxito: reinicia el contador, registra el acceso y entrega el token
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(now);

        Long clientId = clientRepository.findByUserId(user.getId()).map(Client::getId).orElse(null);
        String token = tokenService.generate(user, clientId);

        return new LoginResponse(token, "Bearer", tokenService.expiresInSeconds(),
                user.getUsername(), user.getRole().getName(), user.isMustChangePassword());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario " + username));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La nueva contraseña debe ser distinta de la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
    }
}