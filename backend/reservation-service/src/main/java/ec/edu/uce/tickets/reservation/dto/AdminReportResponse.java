package ec.edu.uce.tickets.reservation.dto;

import ec.edu.uce.tickets.reservation.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Reporte consolidado del administrador (HU-21). */
public record AdminReportResponse(Filters filters, LocalDateTime generatedAt, Summary summary,
                                  List<MatchReport> matches) {

    public record Filters(LocalDate from, LocalDate to, Long matchId, ReservationStatus status) {
    }

    public record Summary(long totalReservations, Map<ReservationStatus, Long> byStatus,
                          long ticketsSold, long ticketsAnnulled) {
    }

    /** match y occupancy son null si match-service no respondió. */
    public record MatchReport(Long matchId, MatchSummary match, long reservations, Occupancy occupancy) {
    }

    public record Occupancy(int capacity, long occupied, double percentage, List<SectionReport> sections) {
    }

    public record SectionReport(String name, String sectionType, int capacity, long occupied, double percentage) {
    }
}