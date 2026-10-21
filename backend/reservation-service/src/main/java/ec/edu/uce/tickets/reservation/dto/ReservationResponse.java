package ec.edu.uce.tickets.reservation.dto;

import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.entity.SeatLock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID publicId,
        String code,
        Long matchId,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        List<Long> seatIds,
        List<TicketResponse> tickets
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getPublicId(),
                reservation.getCode(),
                reservation.getMatchId(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getExpiresAt(),
                reservation.getConfirmedAt(),
                reservation.getCancelledAt(),
                reservation.getSeatLocks().stream().map(SeatLock::getSeatId).sorted().toList(),
                reservation.getTickets().stream().map(TicketResponse::from).toList()
        );
    }
}