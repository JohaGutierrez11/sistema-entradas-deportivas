package ec.edu.uce.tickets.reservation.job;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.service.ReservationWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tarea programada: vence las reservas PENDIENTE que pasaron su tiempo de retención (HU-17). */
@Slf4j
@Component
public class ReservationExpirationJob {

    private final ReservationWriter reservationWriter;
    private final AuditService auditService;

    public ReservationExpirationJob(ReservationWriter reservationWriter, AuditService auditService) {
        this.reservationWriter = reservationWriter;
        this.auditService = auditService;
    }

    public void run() {
        try {
            List<String> codes = reservationWriter.expireAllPending();
            if (!codes.isEmpty()) {
                log.info("Reservas expiradas automáticamente ({}): {}", codes.size(), codes);
                codes.forEach(code -> auditService.success(AuditAction.RESERVA_EXPIRADA, "RESERVA", code,
                        "Venció el tiempo de retención"));
            }
        } catch (Exception e) {
            // Si una ejecución falla (por ejemplo, la base no responde), la siguiente lo vuelve a intentar
            log.error("Falló la expiración automática de reservas", e);
        }
    }
}