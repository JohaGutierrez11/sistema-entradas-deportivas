package ec.edu.uce.tickets.reservation.audit;

import ec.edu.uce.tickets.reservation.audit.AuditWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Registro de auditoría (HU-34). SIN transacción: si falla, se anota en el log y el negocio sigue. */
@Slf4j
@Service
public class AuditService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final String SYSTEM = "SISTEMA";

    private final AuditWriter auditWriter;

    public AuditService(AuditWriter auditWriter) {
        this.auditWriter = auditWriter;
    }

    /** Acción exitosa del usuario autenticado (se toma del token). */
    public void success(AuditAction action, String entityType, String entityId, String detail) {
        record(null, action, entityType, entityId, AuditResult.EXITO, detail);
    }

    /** Con usuario explícito: para el login, donde todavía no hay token. */
    public void record(String username, AuditAction action, String entityType, String entityId,
                       AuditResult result, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setOccurredAt(LocalDateTime.now(ECUADOR));
            entry.setUsername(truncate(username != null ? username : currentUsername(), 50));
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(truncate(entityId, 64));
            entry.setResult(result);
            entry.setDetail(truncate(detail, 250));
            entry.setIpAddress(currentIp());
            auditWriter.write(entry);
        } catch (RuntimeException e) {
            // La auditoría nunca debe tumbar la operación del usuario
            log.error("No se pudo registrar la auditoría {} sobre {} {}", action, entityType, entityId, e);
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return SYSTEM;
        }
        return auth.getName();   // el "sub" del JWT
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;   // tarea programada: no hay petición HTTP
    }

    private String truncate(String text, int max) {
        return (text == null || text.length() <= max) ? text : text.substring(0, max);
    }

}