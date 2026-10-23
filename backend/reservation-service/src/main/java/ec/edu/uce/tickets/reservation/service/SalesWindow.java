package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Ventana de venta online de un partido (HU-40).
 *
 * La venta está abierta si el partido está PROGRAMADO o EN_CURSO **y** todavía no llega la hora de cierre:
 * inicio del partido + SALES_CLOSE_OFFSET_MINUTES (positivo = después del inicio, negativo = antes).
 * Se mira la hora y no solo el estado porque el estado lo cambia una persona: si el admin se olvida
 * de presionar "Iniciar", la venta igual cierra a tiempo.
 */
@Component
public class SalesWindow {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final Set<String> SELLING_STATUSES = Set.of("PROGRAMADO", "EN_CURSO");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ParameterService parameterService;

    public SalesWindow(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    /** Hora a la que deja de venderse (hora de Ecuador). */
    public LocalDateTime closeAt(LocalDate matchDate, LocalTime matchTime) {
        return LocalDateTime.of(matchDate, matchTime)
                .plusMinutes(parameterService.getInt("SALES_CLOSE_OFFSET_MINUTES"));
    }

    public boolean isOpen(String matchStatus, LocalDate matchDate, LocalTime matchTime) {
        return SELLING_STATUSES.contains(matchStatus)
                && LocalDateTime.now(ECUADOR).isBefore(closeAt(matchDate, matchTime));
    }

    /**
     * Lanza 422 si la venta está cerrada; si está abierta, devuelve la hora de cierre.
     * @param action "reservas" o "confirmaciones", solo para el mensaje
     */
    public LocalDateTime assertOpen(String matchStatus, LocalDate matchDate, LocalTime matchTime, String action) {
        if (!SELLING_STATUSES.contains(matchStatus)) {
            throw new BusinessRuleException("El partido está " + matchStatus + " y no admite " + action);
        }
        LocalDateTime closeAt = closeAt(matchDate, matchTime);
        if (!LocalDateTime.now(ECUADOR).isBefore(closeAt)) {
            throw new BusinessRuleException("La venta online de este partido cerró el " + closeAt.format(TIME));
        }
        return closeAt;
    }
}
