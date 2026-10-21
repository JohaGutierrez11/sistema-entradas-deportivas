package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.client.MatchInfo;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.dto.AvailabilityResponse;
import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;
import ec.edu.uce.tickets.reservation.repository.SeatLockRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
public class AvailabilityService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final MatchServiceClient matchServiceClient;
    private final SeatLockRepository seatLockRepository;
    private final ParameterService parameterService;

    public AvailabilityService(MatchServiceClient matchServiceClient, SeatLockRepository seatLockRepository,
                               ParameterService parameterService) {
        this.matchServiceClient = matchServiceClient;
        this.seatLockRepository = seatLockRepository;
        this.parameterService = parameterService;
    }

    // Sin @Transactional a propósito: no se mantiene una conexión a la base abierta
    // mientras se espera la respuesta de otro servicio
    public AvailabilityResponse getAvailability(Long matchId) {
        MatchInfo match = matchServiceClient.getMatch(matchId);   // 404 o 503

        List<AvailabilityResponse.OccupiedSeat> occupied = seatLockRepository
                .findOccupiedSeats(matchId, SeatLockStatus.RESERVED, LocalDateTime.now(ECUADOR))
                .stream()
                .map(lock -> new AvailabilityResponse.OccupiedSeat(lock.getSeatId(), lock.getStatus()))
                .sorted(Comparator.comparing(AvailabilityResponse.OccupiedSeat::seatId))
                .toList();

        return new AvailabilityResponse(match.id(), match.status(), occupied,
                parameterService.getInt("AVAILABILITY_REFRESH_SECONDS"));
    }
}