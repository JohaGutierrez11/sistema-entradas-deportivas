package ec.edu.uce.tickets.auth.service;

import ec.edu.uce.tickets.auth.dto.RegisterRequest;
import ec.edu.uce.tickets.auth.dto.RegisterResponse;
import ec.edu.uce.tickets.auth.entity.*;
import ec.edu.uce.tickets.auth.exception.DuplicateResourceException;
import ec.edu.uce.tickets.auth.repository.AppUserRepository;
import ec.edu.uce.tickets.auth.repository.ClientRepository;
import ec.edu.uce.tickets.auth.repository.RoleRepository;
import ec.edu.uce.tickets.auth.security.CredentialGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CLIENT_ROLE = "CLIENTE";

    private final AppUserRepository appUserRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;
    private final CredentialsMailService mailService;

    public AuthService(AppUserRepository appUserRepository, ClientRepository clientRepository,
                       RoleRepository roleRepository, CredentialGenerator credentialGenerator,
                       PasswordEncoder passwordEncoder, CredentialsMailService mailService) {
        this.appUserRepository = appUserRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.credentialGenerator = credentialGenerator;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
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
}