package ec.edu.uce.tickets.match.dto;

/** Nombre legible de un asiento: lo que el aficionado busca en el estadio. */
public record SeatLabelResponse(Long seatId, String sectionName, String rowLetter, Integer seatNumber) {
}
