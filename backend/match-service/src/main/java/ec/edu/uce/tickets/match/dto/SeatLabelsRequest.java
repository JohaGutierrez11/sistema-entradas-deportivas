package ec.edu.uce.tickets.match.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Asientos de los que se quiere el nombre legible (sección, fila, número). */
public record SeatLabelsRequest(@NotEmpty @Size(max = 100) Set<@NotNull Long> seatIds) {
}
