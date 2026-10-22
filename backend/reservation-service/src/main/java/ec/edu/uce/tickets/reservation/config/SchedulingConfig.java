package ec.edu.uce.tickets.reservation.config;

import ec.edu.uce.tickets.reservation.job.ReservationExpirationJob;
import ec.edu.uce.tickets.reservation.service.ParameterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;

@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    private static final int DEFAULT_SECONDS = 60;

    private final ReservationExpirationJob expirationJob;
    private final ParameterService parameterService;

    public SchedulingConfig(ReservationExpirationJob expirationJob, ParameterService parameterService) {
        this.expirationJob = expirationJob;
        this.parameterService = parameterService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        // Antes de CADA ejecución se vuelve a leer el parámetro: se puede cambiar sin reiniciar
        Trigger trigger = context -> {
            Instant last = context.lastCompletion();
            Instant base = (last != null) ? last : Instant.now();
            return base.plusSeconds(intervalSeconds());
        };
        registrar.addTriggerTask(expirationJob::run, trigger);
    }

    private int intervalSeconds() {
        try {
            int seconds = parameterService.getInt("EXPIRATION_JOB_SECONDS");
            if (seconds > 0) {
                return seconds;
            }
            log.warn("EXPIRATION_JOB_SECONDS = {} no es válido; se usan {} s", seconds, DEFAULT_SECONDS);
        } catch (Exception e) {
            log.warn("No se pudo leer EXPIRATION_JOB_SECONDS; se usan {} s", DEFAULT_SECONDS);
        }
        return DEFAULT_SECONDS;
    }
}