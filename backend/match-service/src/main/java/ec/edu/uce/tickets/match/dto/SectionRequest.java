package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.SectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SectionRequest(
        @NotBlank @Size(min = 2, max = 50)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'()&-]+$",
                message = "solo admite letras, números, espacios y . , ' ( ) & -")
        String name,
        @Size(max = 250) String description,
        @NotNull SectionType sectionType,
        @NotNull @Min(1) @Max(26) Integer rowCount,
        @NotNull @Min(1) @Max(100) Integer seatsPerRow,
        @Valid SectionBoxRequest box
) {
}