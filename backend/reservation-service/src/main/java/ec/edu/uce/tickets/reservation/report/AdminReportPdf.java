package ec.edu.uce.tickets.reservation.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.uce.tickets.reservation.dto.AdminReportResponse;
import ec.edu.uce.tickets.reservation.dto.AdminReportResponse.*;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Dibuja el reporte consolidado del administrador. Solo presentación. */
@Component
public class AdminReportPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font KPI_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font KPI_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font MATCH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(44, 44, 42));

    private static final Color HEADER_BG = new Color(12, 68, 124);     // azul marino
    private static final Color KPI_BG = new Color(230, 241, 251);      // azul hielo
    private static final Color BAR_EMPTY = new Color(230, 241, 251);   // azul muy claro
    private static final Color BAR_LOW = new Color(133, 183, 235);     // < 50 %
    private static final Color BAR_MID = new Color(55, 138, 221);      // 50–89 %
    private static final Color BAR_HIGH = new Color(24, 95, 165);      // ≥ 90 %

    public byte[] build(AdminReportResponse report, String adminUsername) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);   // horizontal
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Reporte consolidado de reservas", TITLE));
            document.add(new Paragraph("Filtros: " + filtersText(report.filters()), SUBTITLE));
            document.add(new Paragraph("Generado por " + adminUsername + " el "
                    + report.generatedAt().format(DATE_TIME), SUBTITLE));
            document.add(Chunk.NEWLINE);

            document.add(kpis(report.summary()));
            document.add(new Paragraph(byStatusText(report.summary()), SUBTITLE));
            document.add(Chunk.NEWLINE);

            if (report.matches().isEmpty()) {
                document.add(new Paragraph("Ninguna reserva coincide con los filtros.", CELL));
            }
            for (MatchReport match : report.matches()) {
                addMatch(document, match);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private String filtersText(Filters f) {
        return (f.from() != null ? "desde " + f.from().format(DATE) : "sin fecha inicial")
                + "  ·  " + (f.to() != null ? "hasta " + f.to().format(DATE) : "sin fecha final")
                + "  ·  " + (f.matchId() != null ? "partido #" + f.matchId() : "todos los partidos")
                + "  ·  " + (f.status() != null ? "estado " + f.status() : "todos los estados");
    }

    /** Tres tarjetas con los números principales. */
    private PdfPTable kpis(Summary summary) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(kpi("Reservas", summary.totalReservations()));
        table.addCell(kpi("Entradas vendidas", summary.ticketsSold()));
        table.addCell(kpi("Entradas anuladas", summary.ticketsAnnulled()));
        return table;
    }

    private PdfPCell kpi(String label, long value) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Paragraph(label, KPI_LABEL));
        cell.addElement(new Paragraph(String.valueOf(value), KPI_VALUE));
        cell.setBackgroundColor(KPI_BG);
        cell.setBorderColor(Color.WHITE);
        cell.setBorderWidth(4);
        cell.setPadding(8);
        return cell;
    }

    private String byStatusText(Summary summary) {
        return summary.byStatus().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("   ·   "));
    }

    private void addMatch(Document document, MatchReport report) throws DocumentException {
        MatchSummary match = report.match();
        String title = match != null
                ? match.name() + "  ·  " + match.matchDate().format(DATE) + " " + match.matchTime().format(TIME)
                + "  ·  " + match.stadiumName()
                : "Partido #" + report.matchId() + " (información no disponible)";

        Paragraph heading = new Paragraph(title, MATCH);
        heading.setSpacingBefore(10);
        document.add(heading);

        Occupancy occupancy = report.occupancy();
        if (occupancy == null) {
            document.add(new Paragraph("Reservas en el filtro: " + report.reservations()
                    + "  ·  Ocupación no disponible (el servicio de partidos no respondió)", SUBTITLE));
            return;
        }
        document.add(new Paragraph("Reservas en el filtro: " + report.reservations()
                + "  ·  Ocupación actual: " + occupancy.occupied() + " / " + occupancy.capacity()
                + " (" + percent(occupancy.percentage()) + ")", SUBTITLE));
        document.add(sectionsTable(occupancy.sections()));
    }

    private PdfPTable sectionsTable(List<SectionReport> sections) {
        PdfPTable table = new PdfPTable(new float[]{3.5f, 2f, 1.2f, 1.2f, 1.2f, 4f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setHeaderRows(1);

        for (String title : List.of("Sección", "Tipo", "Capacidad", "Vendidos", "%", "Ocupación")) {
            PdfPCell cell = new PdfPCell(new Phrase(title, HEADER));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (SectionReport section : sections) {
            table.addCell(cell(section.name()));
            table.addCell(cell(section.sectionType()));
            table.addCell(cell(String.valueOf(section.capacity())));
            table.addCell(cell(String.valueOf(section.occupied())));
            table.addCell(cell(percent(section.percentage())));
            table.addCell(barCell(section.percentage()));
        }
        return table;
    }

    /** Barra de progreso: una tabla de 2 celdas con anchos proporcionales al porcentaje. */
    private PdfPCell barCell(double percentage) {
        float filled = (float) Math.max(0, Math.min(100, percentage));
        PdfPTable bar;
        if (filled <= 0 || filled >= 100) {
            bar = new PdfPTable(1);
            bar.addCell(barPart(filled >= 100 ? barColor(percentage) : BAR_EMPTY));
        } else {
            bar = new PdfPTable(new float[]{filled, 100 - filled});
            bar.addCell(barPart(barColor(percentage)));
            bar.addCell(barPart(BAR_EMPTY));
        }
        bar.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(bar);
        cell.setPadding(5);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell barPart(Color color) {
        PdfPCell part = new PdfPCell();
        part.setBackgroundColor(color);
        part.setBorder(Rectangle.NO_BORDER);
        part.setFixedHeight(8);
        return part;
    }

    private String percent(double value) {
        return String.format(Locale.forLanguageTag("es-EC"), "%.1f %%", value);
    }

    private PdfPCell cell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL));
        cell.setPadding(5);
        return cell;
    }

    private Color barColor(double percentage) {
        if (percentage >= 90) {
            return BAR_HIGH;
        }
        return percentage >= 50 ? BAR_MID : BAR_LOW;
    }
}