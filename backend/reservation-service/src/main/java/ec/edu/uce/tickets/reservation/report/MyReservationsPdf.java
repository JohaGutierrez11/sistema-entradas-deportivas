package ec.edu.uce.tickets.reservation.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.dto.MyReservationItem;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.dto.TicketResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Dibuja "Mis reservas" como PDF. Solo presentación: los datos llegan ya armados. */
@Component
public class MyReservationsPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Color HEADER_BG = new Color(33, 37, 41);

    public byte[] build(String username, List<MyReservationItem> items, LocalDateTime generatedAt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 48, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Mis reservas", TITLE));
            document.add(new Paragraph("Cliente: " + username + "   ·   Generado: "
                    + generatedAt.format(DATE_TIME), SUBTITLE));
            document.add(new Paragraph(summary(items), SUBTITLE));
            document.add(Chunk.NEWLINE);

            if (items.isEmpty()) {
                document.add(new Paragraph("Aún no tienes reservas.", CELL));
            } else {
                document.add(table(items));
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF de reservas", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    /** "Total: 5 · CONFIRMADA: 2 · CANCELADA: 1 · ..." */
    private String summary(List<MyReservationItem> items) {
        Map<String, Long> byStatus = items.stream().collect(Collectors.groupingBy(
                item -> item.reservation().status().name(), TreeMap::new, Collectors.counting()));
        StringBuilder text = new StringBuilder("Total: " + items.size());
        byStatus.forEach((status, count) -> text.append("   ·   ").append(status).append(": ").append(count));
        return text.toString();
    }

    private PdfPTable table(List<MyReservationItem> items) {
        PdfPTable table = new PdfPTable(new float[]{2.3f, 3.6f, 1.8f, 1.7f, 2.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);   // el encabezado se repite en cada página

        for (String title : List.of("Código", "Partido", "Fecha", "Estado", "Entradas")) {
            PdfPCell cell = new PdfPCell(new Phrase(title, HEADER));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (MyReservationItem item : items) {
            ReservationResponse reservation = item.reservation();
            MatchSummary match = item.match();

            table.addCell(cell(reservation.code() + "\nReservada: " + reservation.createdAt().format(DATE_TIME)));
            table.addCell(cell(match != null
                    ? match.name() + "\n" + match.stadiumName()
                    : "Partido #" + reservation.matchId() + "\n(información no disponible)"));
            table.addCell(cell(match != null
                    ? match.matchDate().format(DATE) + "\n" + match.matchTime().format(TIME)
                    : "—"));
            table.addCell(cell(reservation.status().name()));
            table.addCell(cell(seatsText(reservation)));
        }
        return table;
    }

    /** Los asientos salen de los tickets si se confirmó; si no, de los bloqueos vigentes. */
    private String seatsText(ReservationResponse reservation) {
        List<TicketResponse> tickets = reservation.tickets();
        if (!tickets.isEmpty()) {
            String seats = tickets.stream().map(t -> String.valueOf(t.seatId())).collect(Collectors.joining(", "));
            Map<String, Long> byStatus = tickets.stream().collect(Collectors.groupingBy(
                    t -> t.status().name(), TreeMap::new, Collectors.counting()));
            return "Asientos: " + seats + "\nTickets: " + byStatus;
        }
        if (!reservation.seatIds().isEmpty()) {
            return "Asientos: " + reservation.seatIds().stream().map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return "—";
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL));
        cell.setPadding(5);
        return cell;
    }
}