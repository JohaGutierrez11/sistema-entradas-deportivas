package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.SectionBox;

public record BoxResponse(Integer x, Integer y, Integer width, Integer height) {

    /** null si la sección no está ubicada. */
    public static BoxResponse from(SectionBox box) {
        return box == null ? null : new BoxResponse(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    }
}