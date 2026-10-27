package ec.edu.uce.tickets.reservation.dto;

import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(
        Long matchId,
        String matchStatus,
        boolean salesOpen,
        LocalDateTime salesCloseAt,
        List<OccupiedSeat> occupiedSeats,
        int refreshSeconds,
        int maxTicketsPerMatch
) {
    public record OccupiedSeat(Long seatId, SeatLockStatus status) {
    }
}