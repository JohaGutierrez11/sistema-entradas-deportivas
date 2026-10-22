package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.dto.PdfFile;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.dto.TicketResponse;
import ec.edu.uce.tickets.reservation.entity.TicketStatus;
import ec.edu.uce.tickets.reservation.exception.ResourceInUseException;
import ec.edu.uce.tickets.reservation.report.TicketsPdf;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Entradas de una reserva en PDF (HU-37). SIN transacción: pide datos a match-service. */
@Service
public class TicketPdfService {

    private final ReservationService reservationService;
    private final MatchLookup matchLookup;
    private final TicketsPdf ticketsPdf;

    public TicketPdfService(ReservationService reservationService, MatchLookup matchLookup, TicketsPdf ticketsPdf) {
        this.reservationService = reservationService;
        this.matchLookup = matchLookup;
        this.ticketsPdf = ticketsPdf;
    }

    public PdfFile build(UUID publicId, Long clientId, boolean admin) {
        // Reutiliza la regla anti-IDOR de findOne: reserva ajena → 404
        ReservationResponse reservation = reservationService.findOne(publicId, clientId, admin);

        // Solo las entradas válidas: las ANULADAS no se imprimen
        List<TicketResponse> active = reservation.tickets().stream()
                .filter(ticket -> ticket.status() == TicketStatus.ACTIVO)
                .sorted(Comparator.comparing(TicketResponse::seatId))
                .toList();
        if (active.isEmpty()) {
            throw new ResourceInUseException("La reserva no tiene entradas activas para imprimir");
        }

        MatchSummary match = matchLookup.findOrNull(reservation.matchId());
        return new PdfFile("entradas-" + reservation.code() + ".pdf", ticketsPdf.build(reservation, match, active));
    }
}