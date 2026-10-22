package ec.edu.uce.tickets.reservation.client;

import java.util.List;

/** Lo que responde match-service: capacidad y vendidos, total y por sección. */
public record MatchOccupancy(Long matchId, Long stadiumId, int capacity, long occupied, List<Section> sections) {

    public record Section(Long sectionId, String name, String sectionType, int capacity, long occupied) {
    }


}