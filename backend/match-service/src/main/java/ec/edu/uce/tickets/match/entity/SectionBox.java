package ec.edu.uce.tickets.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Recuadro de una sección sobre el plano del estadio (HU-38), en % del plano (0 a 100).
 * Se guarda en las columnas de la tabla section (no tiene tabla propia).
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SectionBox {

    /** La cancha: fija, al centro del plano. Ninguna sección puede quedar encima. */
    public static final SectionBox FIELD = new SectionBox(28, 25, 44, 50);

    @Column(name = "pos_x")
    private Integer x;

    @Column(name = "pos_y")
    private Integer y;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    /**
     * ¿Se superponen? Dos rectángulos se cruzan si se cruzan en el eje X Y en el eje Y.
     * Tocarse en el borde (uno termina en 28 y el otro empieza en 28) NO es superponerse.
     */
    public boolean overlaps(SectionBox other) {
        return x < other.x + other.width && other.x < x + width
                && y < other.y + other.height && other.y < y + height;
    }
}