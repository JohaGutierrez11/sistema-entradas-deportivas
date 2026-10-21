package ec.edu.uce.tickets.reservation.dto;

import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;

import java.util.List;

public record AvailabilityResponse(
        Long matchId,
        String matchStatus,
        List<OccupiedSeat> occupiedSeats,
        int refreshSeconds
) {
    public record OccupiedSeat(Long seatId, SeatLockStatus status) {
    }
}