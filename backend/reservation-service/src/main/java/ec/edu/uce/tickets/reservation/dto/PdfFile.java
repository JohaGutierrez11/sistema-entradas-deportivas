package ec.edu.uce.tickets.reservation.dto;

/** Un archivo listo para descargar: su nombre y su contenido. */
public record PdfFile(String filename, byte[] content) {
}