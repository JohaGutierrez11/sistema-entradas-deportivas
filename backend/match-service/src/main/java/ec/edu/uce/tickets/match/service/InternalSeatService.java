package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.*;
import ec.edu.uce.tickets.match.entity.SportMatch;
import ec.edu.uce.tickets.match.exception.InvalidInternalKeyException;
import ec.edu.uce.tickets.match.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.match.repository.MatchRepository;
import ec.edu.uce.tickets.match.repository.SeatRepository;
import ec.edu.uce.tickets.match.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InternalSeatService {

    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final byte[] internalKey;

    public InternalSeatService(MatchRepository matchRepository, SeatRepository seatRepository, SectionRepository sectionRepository,
                               @Value("${app.internal-key}") String internalKey) {
        this.matchRepository = matchRepository;
        this.seatRepository = seatRepository;
        this.sectionRepository = sectionRepository;
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    public void checkInternalKey(String providedKey) {
        boolean valid = providedKey != null
                && MessageDigest.isEqual(internalKey, providedKey.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new InvalidInternalKeyException();
        }
    }

    @Transactional(readOnly = true)
    public SeatValidationResponse validateSeats(SeatValidationRequest request) {
        SportMatch match = matchRepository.findById(request.matchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el partido con el id " + request.matchId()));

        Long stadiumId = match.getStadium().getId();
        List<Long> valid = seatRepository.findValidSeatIds(stadiumId, request.seatIds())
                .stream().sorted().toList();
        List<Long> invalid = request.seatIds().stream()
                .filter(id -> !valid.contains(id))
                .sorted()
                .toList();

        return new SeatValidationResponse(match.getId(), match.getStatus(),
                match.getMatchDate(), match.getMatchTime(), stadiumId, valid, invalid);
    }

    @Transactional(readOnly = true)
    public MatchOccupancyResponse occupancy(Long matchId, OccupancyRequest request) {
        SportMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido con el id " + matchId));
        Long stadiumId = match.getStadium().getId();

        // sectionId → cuántos vendidos. Si no hay vendidos, no se consulta (IN vacío)
        Map<Long, Long> occupiedBySection = new HashMap<>();
        if (!request.seatIds().isEmpty()) {
            for (Object[] row : seatRepository.countBySection(stadiumId, request.seatIds())) {
                occupiedBySection.put((Long) row[0], (Long) row[1]);
            }
        }

        // Todas las secciones del estadio, también las que tienen 0 vendidos
        List<SectionOccupancy> sections = sectionRepository.findByStadiumIdOrderByIdAsc(stadiumId).stream()
                .map(section -> new SectionOccupancy(section.getId(), section.getName(), section.getSectionType(),
                        section.getCapacity(), occupiedBySection.getOrDefault(section.getId(), 0L)))
                .toList();

        int capacity = sections.stream().mapToInt(SectionOccupancy::capacity).sum();
        long occupied = sections.stream().mapToLong(SectionOccupancy::occupied).sum();
        return new MatchOccupancyResponse(matchId, stadiumId, capacity, occupied, sections);
    }
}