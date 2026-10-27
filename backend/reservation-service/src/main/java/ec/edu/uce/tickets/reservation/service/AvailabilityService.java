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
    private final SalesWindow salesWindow;

    public AvailabilityService(MatchServiceClient matchServiceClient, SeatLockRepository seatLockRepository,
                               ParameterService parameterService, SalesWindow salesWindow) {
        this.matchServiceClient = matchServiceClient;
        this.seatLockRepository = seatLockRepository;
        this.parameterService = parameterService;
        this.salesWindow = salesWindow;
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

        // El front no repite la regla: el backend le dice si la venta está abierta y hasta cuándo
        return new AvailabilityResponse(match.id(), match.status(),
                salesWindow.isOpen(match.status(), match.matchDate(), match.matchTime()),
                salesWindow.closeAt(match.matchDate(), match.matchTime()),
                occupied,
                parameterService.getInt("AVAILABILITY_REFRESH_SECONDS"),
                parameterService.getInt("MAX_TICKETS_PER_MATCH"));   // el front ya no fija el 6 a mano
    }
}