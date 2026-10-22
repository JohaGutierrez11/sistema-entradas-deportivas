package ec.edu.uce.tickets.match.dto;

import java.util.List;

public record MatchOccupancyResponse(Long matchId, Long stadiumId, int capacity, long occupied,
                                     List<SectionOccupancy> sections) {
}