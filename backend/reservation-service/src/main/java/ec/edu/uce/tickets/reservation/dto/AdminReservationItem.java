package ec.edu.uce.tickets.reservation.dto;

/**
 * Una reserva vista por el administrador: la misma ReservationResponse más el id del cliente.
 * Va aparte para no exponer el id interno del cliente en las respuestas que recibe el propio cliente.
 */
public record AdminReservationItem(Long clientId, ReservationResponse reservation) {
}
