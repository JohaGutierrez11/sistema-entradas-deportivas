package ec.edu.uce.tickets.match.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.edu.uce.tickets.match.entity.SectionBox;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Recuadro de una sección, en % del plano (0 a 100). */
public record SectionBoxRequest(
        @NotNull @Min(0) @Max(95) Integer x,
        @NotNull @Min(0) @Max(95) Integer y,
        @NotNull @Min(5) @Max(100) Integer width,
        @NotNull @Min(5) @Max(100) Integer height
) {
    /** Validación cruzada: cada número puede estar bien, pero juntos salirse del plano. */
    @JsonIgnore
    @AssertTrue(message = "el recuadro se sale del plano: x + ancho y y + alto no pueden pasar de 100")
    public boolean isInsidePlan() {
        if (x == null || y == null || width == null || height == null) {
            return true;   // de los null ya se encargan los @NotNull
        }
        return x + width <= 100 && y + height <= 100;
    }

    public SectionBox toBox() {
        return new SectionBox(x, y, width, height);
    }
}