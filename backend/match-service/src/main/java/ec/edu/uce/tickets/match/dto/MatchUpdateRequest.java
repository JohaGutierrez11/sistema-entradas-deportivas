package ec.edu.uce.tickets.match.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ec.edu.uce.tickets.match.entity.MatchStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record MatchUpdateRequest(
        @NotBlank @Size(min = 3, max = 100)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,'()&-]+$",
                message = "solo admite letras, números, espacios y . , ' ( ) & -")
        String name,
        @Size(max = 250) String description,
        @NotNull LocalDate matchDate,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime matchTime,
        @NotNull @Min(1) Long stadiumId,
        @NotNull MatchStatus status
) {
}