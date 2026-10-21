package ec.edu.uce.tickets.reservation.dto;

import ec.edu.uce.tickets.reservation.entity.Ticket;
import ec.edu.uce.tickets.reservation.entity.TicketStatus;

import java.util.UUID;

public record TicketResponse(UUID publicId, Long seatId, String qrCode, TicketStatus status) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getPublicId(), ticket.getSeatId(), ticket.getQrCode(), ticket.getStatus());
    }
}