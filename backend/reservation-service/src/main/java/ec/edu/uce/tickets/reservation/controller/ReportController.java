package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.AdminReportResponse;
import ec.edu.uce.tickets.reservation.dto.PdfFile;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.service.ReservationReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Reportes")
@RestController
@RequestMapping("/reservation-api/v1/reports")
public class ReportController {

    private final ReservationReportService reportService;

    public ReportController(ReservationReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reservations")
    public AdminReportResponse reservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Long matchId,
            @RequestParam(required = false) ReservationStatus status) {
        return reportService.build(from, to, matchId, status);
    }

    @GetMapping("/reservations/pdf")
    public ResponseEntity<byte[]> reservationsPdf(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @Min(1) Long matchId,
            @RequestParam(required = false) ReservationStatus status) {
        PdfFile file = reportService.buildPdf(from, to, matchId, status, jwt.getSubject());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }
}