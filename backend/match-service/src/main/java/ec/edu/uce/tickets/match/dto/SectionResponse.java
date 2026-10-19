package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.Section;
import ec.edu.uce.tickets.match.entity.SectionType;

public record SectionResponse(
        Long id,
        Long stadiumId,
        String name,
        String description,
        SectionType sectionType,
        Integer rowCount,
        Integer seatsPerRow,
        Integer capacity
) {
    public static SectionResponse from(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getStadium().getId(),
                section.getName(),
                section.getDescription(),
                section.getSectionType(),
                section.getRowCount(),
                section.getSeatsPerRow(),
                section.getCapacity()
        );
    }
}