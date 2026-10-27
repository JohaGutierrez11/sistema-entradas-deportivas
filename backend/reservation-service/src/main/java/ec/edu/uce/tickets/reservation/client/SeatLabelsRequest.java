package ec.edu.uce.tickets.reservation.client;

import java.util.Collection;

public record SeatLabelsRequest(Collection<Long> seatIds) {
}
