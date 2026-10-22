package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.CreateReservationRequest;
import ec.edu.uce.tickets.reservation.dto.PdfFile;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.service.ReservationService;
import ec.edu.uce.tickets.reservation.service.TicketPdfService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Reservas")
@RestController
@RequestMapping("/reservation-api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final TicketPdfService ticketPdfService;

    public ReservationController(ReservationService reservationService, TicketPdfService ticketPdfService) {
        this.reservationService = reservationService;
        this.ticketPdfService = ticketPdfService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse created = reservationService.create(clientIdOf(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{publicId}")
                .buildAndExpand(created.publicId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{publicId}")
    public ReservationResponse findOne(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        return reservationService.findOne(publicId, clientIdOf(jwt), isAdmin(jwt));
    }

    /** El cliente SIEMPRE sale del token, nunca del body. */
    private Long clientIdOf(Jwt jwt) {
        return jwt.getClaim("clientId") instanceof Number number ? number.longValue() : null;
    }

    private boolean isAdmin(Jwt jwt) {
        return "ADMIN".equals(jwt.getClaimAsString("role"));
    }

    @PostMapping("/{publicId}/confirm")
    public ReservationResponse confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        return reservationService.confirm(publicId, clientIdOf(jwt));
    }

    @PostMapping("/{publicId}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        return reservationService.cancel(publicId, clientIdOf(jwt), isAdmin(jwt));
    }

    @GetMapping("/{publicId}/tickets/pdf")
    public ResponseEntity<byte[]> ticketsPdf(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID publicId) {
        PdfFile file = ticketPdfService.build(publicId, clientIdOf(jwt), isAdmin(jwt));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }
}