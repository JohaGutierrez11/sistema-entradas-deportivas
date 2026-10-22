package ec.edu.uce.tickets.reservation.job;

import ec.edu.uce.tickets.reservation.service.ReservationWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tarea programada: vence las reservas PENDIENTE que pasaron su tiempo de retención (HU-17). */
@Slf4j
@Component
public class ReservationExpirationJob {

    private final ReservationWriter reservationWriter;

    public ReservationExpirationJob(ReservationWriter reservationWriter) {
        this.reservationWriter = reservationWriter;
    }

    public void run() {
        try {
            List<String> codes = reservationWriter.expireAllPending();
            if (!codes.isEmpty()) {
                log.info("Reservas expiradas automáticamente ({}): {}", codes.size(), codes);
            }
        } catch (Exception e) {
            // Si una ejecución falla (por ejemplo, la base no responde), la siguiente lo vuelve a intentar
            log.error("Falló la expiración automática de reservas", e);
        }
    }
}