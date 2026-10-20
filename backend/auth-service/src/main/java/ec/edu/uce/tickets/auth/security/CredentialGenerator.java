package ec.edu.uce.tickets.auth.security;

import ec.edu.uce.tickets.auth.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;

@Component
public class CredentialGenerator {

    private static final int MIN_USERNAME_LENGTH = 4;
    private static final int MAX_BASE_LENGTH = 45;   // deja espacio para el número
    private static final int PASSWORD_LENGTH = 12;
    // Sin caracteres que se confunden al leer: 0/O, 1/l/I
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789*#$%&";

    private final SecureRandom random = new SecureRandom();
    private final AppUserRepository appUserRepository;

    public CredentialGenerator(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /** María López → "mlopez"; si ya existe → "mlopez2", "mlopez3"... */
    public String generateUsername(String firstName, String lastName) {
        String base = normalize(firstName).charAt(0) + normalize(lastName);
        if (base.length() > MAX_BASE_LENGTH) {
            base = base.substring(0, MAX_BASE_LENGTH);
        }

        String candidate = base;
        int counter = 1;
        while (candidate.length() < MIN_USERNAME_LENGTH
                || appUserRepository.existsByUsernameIgnoreCase(candidate)) {
            counter++;
            candidate = base + counter;
        }
        return candidate;
    }

    /** Contraseña aleatoria de 12 caracteres para el primer acceso. */
    public String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    /** "  Peña " → "pena" ; "De la Torre" → "delatorre" ; "O'Brien" → "obrien" */
    private String normalize(String text) {
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")        // quita tildes, diéresis y la virgulilla de la ñ
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");    // quita espacios, apóstrofos, guiones...
    }
}