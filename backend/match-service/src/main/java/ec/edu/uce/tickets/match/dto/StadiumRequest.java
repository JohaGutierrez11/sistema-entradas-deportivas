package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.VenueType;
import jakarta.validation.constraints.*;

public record StadiumRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'()&-]+$",
                message = "solo admite letras, números, espacios y . , ' ( ) & -")
        String name,
        @Size(max = 250) String description,
        @NotBlank @Size(min = 3, max = 200)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'()&#/°-]+$",
                message = "solo admite letras, números, espacios y . , ' ( ) & # / ° -")
        String location,
        @NotNull @Min(1) @Max(200000) Integer totalCapacity,
        /** Opcional: si no se envía, se crea como ESTADIO (y al editar se conserva el tipo actual). */
        VenueType venueType
) {
}
