package ec.edu.uce.tickets.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CredentialsMailService {

    private final JavaMailSender mailSender;

    public CredentialsMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCredentials(String to, String fullName, String username, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@entradas.uce.edu.ec");
        message.setTo(to);
        message.setSubject("Tus credenciales de acceso - Sistema de Entradas");
        message.setText("""
                Hola %s,

                Tu cuenta fue creada correctamente.

                Usuario: %s
                Contraseña de primer acceso: %s

                Por seguridad, deberás cambiar esta contraseña la primera vez que inicies sesión.
                """.formatted(fullName, username, temporaryPassword));
        mailSender.send(message);
    }
}