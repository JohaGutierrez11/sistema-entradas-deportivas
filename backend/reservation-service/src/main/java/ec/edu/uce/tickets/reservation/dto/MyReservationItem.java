package ec.edu.uce.tickets.reservation.dto;

/** Una fila del historial: la reserva y, si match-service respondió, su partido. */
public record MyReservationItem(ReservationResponse reservation, MatchSummary match) {
}