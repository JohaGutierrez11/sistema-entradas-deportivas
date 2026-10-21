package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.entity.Ticket;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QrCodeGenerator qrCodeGenerator;

    public TicketService(TicketRepository ticketRepository, QrCodeGenerator qrCodeGenerator) {
        this.ticketRepository = ticketRepository;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    // readOnly: ticket.getReservation() es LAZY y necesita la sesión abierta
    @Transactional(readOnly = true)
    public byte[] getQrImage(UUID publicId, Long clientId, boolean admin) {
        Ticket ticket = ticketRepository.findByPublicId(publicId)
                .filter(t -> admin || t.getReservation().getClientId().equals(clientId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe el ticket " + publicId));
        return qrCodeGenerator.toPng(ticket.getQrCode());
    }
}