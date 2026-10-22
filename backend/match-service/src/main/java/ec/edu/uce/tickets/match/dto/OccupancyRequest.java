package ec.edu.uce.tickets.match.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** Asientos vendidos de un partido, para contarlos por sección. Puede venir vacío. */
public record OccupancyRequest(@NotNull Set<@NotNull Long> seatIds) {
}