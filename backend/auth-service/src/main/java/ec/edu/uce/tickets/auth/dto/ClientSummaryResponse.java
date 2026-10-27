package ec.edu.uce.tickets.auth.dto;

import ec.edu.uce.tickets.auth.entity.Client;

/**
 * Lo mínimo que el administrador necesita para identificar a un cliente (HU-44): nombre, usuario,
 * correo y los últimos 4 dígitos de la cédula para verificarlo por teléfono. Minimización de datos:
 * no se exponen la cédula completa, la dirección ni el teléfono.
 */
public record ClientSummaryResponse(Long id, String fullName, String username, String email, String cedulaLast4) {

    public static ClientSummaryResponse from(Client client) {
        String fullName = String.join(" ", client.getFirstName(),
                client.getMiddleName() == null ? "" : client.getMiddleName(), client.getLastName())
                .replaceAll("\\s+", " ").trim();
        String cedula = client.getCedula();
        return new ClientSummaryResponse(client.getId(), fullName, client.getUser().getUsername(), client.getEmail(),
                cedula.substring(Math.max(0, cedula.length() - 4)));
    }
}
