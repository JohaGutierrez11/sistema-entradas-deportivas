package ec.edu.uce.tickets.reservation.client;

/** Nombre legible de un asiento, según match-service. */
public record SeatLabel(Long seatId, String sectionName, String rowLetter, Integer seatNumber) {

    /** "Fila A · Asiento 3" (la sección va en otra línea de la entrada). */
    public String rowAndNumber() {
        return "Fila " + rowLetter + "  ·  Asiento " + seatNumber;
    }
}
