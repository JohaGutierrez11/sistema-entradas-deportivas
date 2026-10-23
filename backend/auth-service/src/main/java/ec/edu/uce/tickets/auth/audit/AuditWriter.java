package ec.edu.uce.tickets.auth.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Guarda en una transacción NUEVA: sobrevive aunque la operación principal haga rollback. */
@Component
public class AuditWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditLog entry) {
        auditLogRepository.save(entry);
    }
}