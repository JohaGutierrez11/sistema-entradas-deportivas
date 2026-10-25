package ec.edu.uce.tickets.match.dto;

import ec.edu.uce.tickets.match.entity.Stadium;
import ec.edu.uce.tickets.match.entity.VenueType;

public record StadiumResponse(
        Long id,
        String name,
        String description,
        String location,
        Integer totalCapacity,
        VenueType venueType
) {
    public static StadiumResponse from(Stadium stadium){
        return new StadiumResponse(
                stadium.getId(),
                stadium.getName(),
                stadium.getDescription(),
                stadium.getLocation(),
                stadium.getTotalCapacity(),
                stadium.getVenueType()
        );
    }
}
