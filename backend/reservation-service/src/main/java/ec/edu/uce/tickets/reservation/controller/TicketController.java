package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Tickets")
@RestController
@RequestMapping("/reservation-api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping(value = "/{publicId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        Long clientId = jwt.getClaim("clientId") instanceof Number n ? n.longValue() : null;
        boolean admin = "ADMIN".equals(jwt.getClaimAsString("role"));
        return ticketService.getQrImage(publicId, clientId, admin);
    }
}