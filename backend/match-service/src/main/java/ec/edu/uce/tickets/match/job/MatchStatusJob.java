package ec.edu.uce.tickets.match.job;

import ec.edu.uce.tickets.match.service.MatchLifecycleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada: cada minuto (y al arrancar) inicia y finaliza los partidos según la hora.
 * Misma idea que la expiración de reservas: el tiempo pasa aunque nadie haga clic.
 */
@Slf4j
@Component
public class MatchStatusJob {

    private final MatchLifecycleService lifecycleService;

    public MatchStatusJob(MatchLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${app.match.status-check-ms:60000}")
    public void run() {
        try {
            MatchLifecycleService.Result result = lifecycleService.advance();
            if (result.changedAnything()) {
                log.info("Partidos actualizados por la hora: {} iniciados, {} finalizados",
                        result.started(), result.finished());
            }
        } catch (RuntimeException ex) {
            // Si la base no responde, se reintenta en la próxima vuelta; la tarea no debe morir
            log.error("No se pudo actualizar el estado de los partidos", ex);
        }
    }
}
