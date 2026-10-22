package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.MyReservationItem;
import ec.edu.uce.tickets.reservation.service.MyReservationsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Mis reservas")
@RestController
@RequestMapping("/reservation-api/v1/reservations/me")
public class MyReservationsController {

    private final MyReservationsService myReservationsService;

    public MyReservationsController(MyReservationsService myReservationsService) {
        this.myReservationsService = myReservationsService;
    }

    @GetMapping
    public List<MyReservationItem> findMine(@AuthenticationPrincipal Jwt jwt) {
        return myReservationsService.findMine(clientIdOf(jwt));
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> findMineAsPdf(@AuthenticationPrincipal Jwt jwt) {
        byte[] pdf = myReservationsService.findMineAsPdf(clientIdOf(jwt), jwt.getSubject());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mis-reservas.pdf").build().toString())
                .body(pdf);
    }

    private Long clientIdOf(Jwt jwt) {
        return jwt.getClaim("clientId") instanceof Number number ? number.longValue() : null;
    }
}