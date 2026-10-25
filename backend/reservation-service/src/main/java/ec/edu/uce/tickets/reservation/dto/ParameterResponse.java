package ec.edu.uce.tickets.reservation.dto;

/**
 * Un parámetro del sistema tal como lo ve el administrador (HU-43).
 * min, max y unit salen de las reglas del servicio: el front no las inventa.
 */
public record ParameterResponse(String code, String value, String description, boolean editable,
                                Integer min, Integer max, String unit) {
}
