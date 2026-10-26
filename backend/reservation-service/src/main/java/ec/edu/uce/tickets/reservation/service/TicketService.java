package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.dto.TicketValidationResponse;
import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.entity.TicketStatus;
import ec.edu.uce.tickets.reservation.entity.Ticket;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QrCodeGenerator qrCodeGenerator;

    public TicketService(TicketRepository ticketRepository, QrCodeGenerator qrCodeGenerator) {
        this.ticketRepository = ticketRepository;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    /**
     * HU-47: marca la entrada como UTILIZADA si corresponde a este partido y sigue ACTIVA.
     * Corre en una transacción con la fila bloqueada: dos lecturas simultáneas del mismo QR no pasan las dos.
     */
    @Transactional
    public TicketValidationResponse use(String qrCode, Long matchId) {
        Ticket ticket = ticketRepository.findByQrCodeForUpdate(qrCode.trim()).orElse(null);
        if (ticket == null) {
            return TicketValidationResponse.rejected(TicketValidationResponse.Result.NO_RECONOCIDA,
                    "El código no corresponde a ninguna entrada");
        }
        Reservation reservation = ticket.getReservation();
        if (!reservation.getMatchId().equals(matchId)) {
            return result(ticket, false, TicketValidationResponse.Result.OTRO_PARTIDO,
                    "La entrada es de otro partido (partido #" + reservation.getMatchId() + ")");
        }
        if (ticket.getStatus() == TicketStatus.UTILIZADO) {
            return result(ticket, false, TicketValidationResponse.Result.YA_UTILIZADA,
                    "La entrada ya se usó: alguien ingresó con ella");
        }
        if (ticket.getStatus() == TicketStatus.ANULADO || reservation.getStatus() != ReservationStatus.CONFIRMADA) {
            return result(ticket, false, TicketValidationResponse.Result.ANULADA,
                    "La entrada está anulada: su reserva se canceló");
        }
        ticket.setStatus(TicketStatus.UTILIZADO);
        ticket.setUsedAt(LocalDateTime.now(ZoneId.of("America/Guayaquil")));   // dirty checking
        return result(ticket, true, TicketValidationResponse.Result.VALIDA, "Entrada válida: puede ingresar");
    }

    private static TicketValidationResponse result(Ticket ticket, boolean valid, TicketValidationResponse.Result result,
                                                   String message) {
        return new TicketValidationResponse(valid, result, message, ticket.getPublicId(), ticket.getSeatId(),
                ticket.getReservation().getCode(), ticket.getReservation().getClientId(), ticket.getUsedAt());
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