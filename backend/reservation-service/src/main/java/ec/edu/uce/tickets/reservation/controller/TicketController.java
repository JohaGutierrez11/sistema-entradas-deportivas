package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.TicketValidationRequest;
import ec.edu.uce.tickets.reservation.dto.TicketValidationResponse;
import ec.edu.uce.tickets.reservation.service.AccessControlService;
import ec.edu.uce.tickets.reservation.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final AccessControlService accessControlService;

    public TicketController(TicketService ticketService, AccessControlService accessControlService) {
        this.ticketService = ticketService;
        this.accessControlService = accessControlService;
    }

    /** Control de acceso (HU-47), solo ADMIN: la regla está en SecurityConfig. */
    @PostMapping("/validate")
    public TicketValidationResponse validate(@Valid @RequestBody TicketValidationRequest request) {
        return accessControlService.validate(request.qrCode(), request.matchId());
    }

    @GetMapping(value = "/{publicId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        Long clientId = jwt.getClaim("clientId") instanceof Number n ? n.longValue() : null;
        boolean admin = "ADMIN".equals(jwt.getClaimAsString("role"));
        return ticketService.getQrImage(publicId, clientId, admin);
    }
}