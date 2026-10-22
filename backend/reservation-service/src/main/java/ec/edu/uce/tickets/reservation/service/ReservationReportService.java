package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.client.MatchOccupancy;
import ec.edu.uce.tickets.reservation.dto.AdminReportResponse;
import ec.edu.uce.tickets.reservation.dto.AdminReportResponse.*;
import ec.edu.uce.tickets.reservation.dto.PdfFile;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.dto.TicketResponse;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import ec.edu.uce.tickets.reservation.entity.TicketStatus;
import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import ec.edu.uce.tickets.reservation.report.AdminReportPdf;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/** Reporte consolidado del administrador (HU-21). SIN transacción: consulta a match-service. */
@Service
public class ReservationReportService {

    private final ReservationService reservationService;
    private final MatchLookup matchLookup;
    private final AdminReportPdf adminReportPdf;

    public ReservationReportService(ReservationService reservationService, MatchLookup matchLookup, AdminReportPdf adminReportPdf) {
        this.reservationService = reservationService;
        this.matchLookup = matchLookup;
        this.adminReportPdf = adminReportPdf;
    }

    public AdminReportResponse build(LocalDate from, LocalDate to, Long matchId, ReservationStatus status) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final");
        }

        // (a) Nuestra base, con transacción (en OTRA clase)
        List<ReservationResponse> reservations = reservationService.search(from, to, matchId, status);

        // (b) Un bloque por partido distinto: datos del partido + ocupación actual (match-service)
        List<MatchReport> matches = reservations.stream()
                .map(ReservationResponse::matchId)
                .distinct()
                .sorted()
                .map(id -> matchReport(id, reservations))
                .toList();

        return new AdminReportResponse(
                new Filters(from, to, matchId, status),
                LocalDateTime.now(ZoneId.of("America/Guayaquil")),
                summary(reservations),
                matches);
    }

    private Summary summary(List<ReservationResponse> reservations) {
        // Todos los estados aparecen, aunque tengan 0: un 0 también es información
        Map<ReservationStatus, Long> byStatus = new EnumMap<>(ReservationStatus.class);
        for (ReservationStatus s : ReservationStatus.values()) {
            byStatus.put(s, 0L);
        }
        reservations.forEach(r -> byStatus.merge(r.status(), 1L, Long::sum));

        List<TicketResponse> tickets = reservations.stream().flatMap(r -> r.tickets().stream()).toList();
        long sold = tickets.stream()
                .filter(t -> t.status() == TicketStatus.ACTIVO || t.status() == TicketStatus.UTILIZADO)
                .count();
        long annulled = tickets.stream().filter(t -> t.status() == TicketStatus.ANULADO).count();

        return new Summary(reservations.size(), byStatus, sold, annulled);
    }

    private MatchReport matchReport(Long matchId, List<ReservationResponse> reservations) {
        long count = reservations.stream().filter(r -> r.matchId().equals(matchId)).count();
        List<Long> soldSeatIds = reservationService.findSoldSeatIds(matchId);
        MatchOccupancy occupancy = matchLookup.occupancyOrNull(matchId, soldSeatIds);

        return new MatchReport(matchId, matchLookup.findOrNull(matchId), count, toOccupancy(occupancy));
    }

    private Occupancy toOccupancy(MatchOccupancy occupancy) {
        if (occupancy == null) {
            return null;
        }
        List<SectionReport> sections = occupancy.sections().stream()
                .map(s -> new SectionReport(s.name(), s.sectionType(), s.capacity(), s.occupied(),
                        percentage(s.occupied(), s.capacity())))
                .toList();
        return new Occupancy(occupancy.capacity(), occupancy.occupied(),
                percentage(occupancy.occupied(), occupancy.capacity()), sections);
    }

    /** 8 de 122 → 6.6 (un decimal). Sin capacidad → 0, nunca división por cero. */
    private double percentage(long part, int total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    public PdfFile buildPdf(LocalDate from, LocalDate to, Long matchId, ReservationStatus status, String adminUsername) {
        AdminReportResponse report = build(from, to, matchId, status);   // el MISMO reporte del JSON
        String filename = "reporte-reservas-" + report.generatedAt().toLocalDate() + ".pdf";
        return new PdfFile(filename, adminReportPdf.build(report, adminUsername));
    }
}