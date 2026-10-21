package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.SeatValidationRequest;
import ec.edu.uce.tickets.match.dto.SeatValidationResponse;
import ec.edu.uce.tickets.match.entity.SportMatch;
import ec.edu.uce.tickets.match.exception.InvalidInternalKeyException;
import ec.edu.uce.tickets.match.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.match.repository.MatchRepository;
import ec.edu.uce.tickets.match.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class InternalSeatService {

    private final MatchRepository matchRepository;
    private final SeatRepository seatRepository;
    private final byte[] internalKey;

    public InternalSeatService(MatchRepository matchRepository, SeatRepository seatRepository,
                               @Value("${app.internal-key}") String internalKey) {
        this.matchRepository = matchRepository;
        this.seatRepository = seatRepository;
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

        return new SeatValidationResponse(match.getId(), match.getStatus(), stadiumId, valid, invalid);
    }
}