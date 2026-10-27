package ec.edu.uce.tickets.reservation.job;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.repository.ReservationRepository;
import ec.edu.uce.tickets.reservation.service.ReservationWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HU-46 · Conciliación: si un partido se canceló en match-service, sus reservas se cancelan aquí y sus
 * entradas quedan ANULADAS.
 *
 * ¿Por qué una tarea que pregunta y no una llamada de match-service a este servicio? Porque así la
 * dependencia sigue en una sola dirección (reservas → partidos) y, si este servicio está caído cuando se
 * cancela el partido, no se pierde nada: en la siguiente vuelta lo encuentra y lo anula (consistencia eventual).
 */
@Slf4j
@Component
public class CancelledMatchJob {

    private static final List<ReservationStatus> ACTIVE = List.of(ReservationStatus.PENDIENTE, ReservationStatus.CONFIRMADA);

    private final ReservationRepository reservationRepository;
    private final MatchServiceClient matchServiceClient;
    private final ReservationWriter reservationWriter;
    private final AuditService auditService;

    public CancelledMatchJob(ReservationRepository reservationRepository, MatchServiceClient matchServiceClient,
                             ReservationWriter reservationWriter, AuditService auditService) {
        this.reservationRepository = reservationRepository;
        this.matchServiceClient = matchServiceClient;
        this.reservationWriter = reservationWriter;
        this.auditService = auditService;
    }

    @Scheduled(initialDelay = 20000, fixedDelayString = "${app.reservations.cancelled-match-check-ms:60000}")
    public void run() {
        for (Long matchId : reservationRepository.findMatchIdsWithStatusIn(ACTIVE)) {
            try {
                if ("CANCELADO".equals(matchServiceClient.getMatch(matchId).status())) {
                    List<String> codes = reservationWriter.cancelAllOfMatch(matchId);
                    log.info("Partido {} cancelado: se anularon {} reservas {}", matchId, codes.size(), codes);
                    codes.forEach(code -> auditService.success(AuditAction.RESERVA_CANCELADA, "RESERVA", code,
                            "Partido " + matchId + " cancelado: reserva cancelada y entradas anuladas"));
                }
            } catch (RuntimeException e) {
                // match-service caído o partido inexistente: se reintenta en la próxima vuelta
                log.warn("No se pudo revisar el partido {}: {}", matchId, e.getMessage());
            }
        }
    }
}
