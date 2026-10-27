package ec.edu.uce.tickets.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nuevo valor de un parámetro. Se recibe como texto y el servicio valida que sea un entero en rango. */
public record UpdateParameterRequest(@NotBlank @Size(max = 100) String value) {
}
