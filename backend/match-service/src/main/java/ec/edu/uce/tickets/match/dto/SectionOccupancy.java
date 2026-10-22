package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.SectionType;

public record SectionOccupancy(Long sectionId, String name, SectionType sectionType, int capacity, long occupied) {
}