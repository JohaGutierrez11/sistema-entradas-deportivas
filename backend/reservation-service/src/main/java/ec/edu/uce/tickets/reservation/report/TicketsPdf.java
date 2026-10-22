package ec.edu.uce.tickets.reservation.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.dto.ReservationResponse;
import ec.edu.uce.tickets.reservation.dto.TicketResponse;
import ec.edu.uce.tickets.reservation.service.QrCodeGenerator;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Dibuja las entradas: una página tamaño A6 por ticket, con su QR. */
@Component
public class TicketsPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
    private static final Font MATCH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font SEAT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.GRAY);
    private static final float QR_SIZE = 160;

    private final QrCodeGenerator qrCodeGenerator;

    public TicketsPdf(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public byte[] build(ReservationResponse reservation, MatchSummary match, List<TicketResponse> tickets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A6, 24, 24, 24, 24);   // A6: tamaño de un pase
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            for (int i = 0; i < tickets.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                addTicketPage(document, reservation, match, tickets.get(i), i + 1, tickets.size());
            }
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF de entradas", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }

    private void addTicketPage(Document document, ReservationResponse reservation, MatchSummary match,
                               TicketResponse ticket, int number, int total) throws DocumentException, IOException {
        document.add(centered("ENTRADA", LABEL, 4));

        if (match != null) {
            document.add(centered(match.name(), MATCH, 2));
            document.add(centered(match.stadiumName(), TEXT, 2));
            document.add(centered(match.matchDate().format(DATE) + "  ·  " + match.matchTime().format(TIME), TEXT, 8));
        } else {
            document.add(centered("Partido #" + reservation.matchId(), MATCH, 8));
        }

        document.add(centered("Asiento " + ticket.seatId(), SEAT, 2));
        document.add(centered("Reserva " + reservation.code(), TEXT, 8));

        Image qr = Image.getInstance(qrCodeGenerator.toPng(ticket.qrCode()));
        qr.scaleAbsolute(QR_SIZE, QR_SIZE);
        qr.setAlignment(Element.ALIGN_CENTER);
        document.add(qr);

        document.add(centered("Entrada " + number + " de " + total + "  ·  Válida para una sola persona", FOOTER, 2));
        document.add(centered("Presenta este código en la puerta del estadio. No lo compartas.", FOOTER, 0));
    }

    private Paragraph centered(String text, Font font, float spacingAfter) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }
}