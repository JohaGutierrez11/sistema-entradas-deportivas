package ec.edu.uce.tickets.match.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editar una sección: solo nombre y descripción.
 * Las filas y los asientos no se cambian: pueden tener entradas vendidas en reservation_db.
 */
public record SectionUpdateRequest(
        @NotBlank @Size(min = 2, max = 50)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'()&-]+$",
                message = "solo admite letras, números, espacios y . , ' ( ) & -")
        String name,
        @Size(max = 250) String description
) {
}
