package ec.edu.uce.tickets.match.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (@Scheduled) de match-service. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
