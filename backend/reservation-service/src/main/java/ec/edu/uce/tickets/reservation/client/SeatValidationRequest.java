package ec.edu.uce.tickets.reservation.client;

import java.util.Collection;

public record SeatValidationRequest(Long matchId, Collection<Long> seatIds) {
}