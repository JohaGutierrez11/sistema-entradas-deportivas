package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditResult;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.client.MatchInfo;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.dto.TicketValidationResponse;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Control de acceso en la puerta del escenario (HU-47): valida el QR de una entrada y la marca como UTILIZADA.
 * SIN @Transactional: primero se consulta a match-service y después se abre la transacción (como al reservar).
 */
@Service
public class AccessControlService {

    /** Se controla el ingreso antes del inicio (se abren puertas) y durante el partido. */
    private static final Set<String> OPEN_STATUSES = Set.of("PROGRAMADO", "EN_CURSO");

    private final MatchServiceClient matchServiceClient;
    private final TicketService ticketService;
    private final AuditService auditService;

    public AccessControlService(MatchServiceClient matchServiceClient, TicketService ticketService,
                                AuditService auditService) {
        this.matchServiceClient = matchServiceClient;
        this.ticketService = ticketService;
        this.auditService = auditService;
    }

    public TicketValidationResponse validate(String qrCode, Long matchId) {
        MatchInfo match = matchServiceClient.getMatch(matchId);   // 404 o 503
        TicketValidationResponse response = OPEN_STATUSES.contains(match.status())
                ? ticketService.use(qrCode, matchId)
                : TicketValidationResponse.rejected(TicketValidationResponse.Result.PARTIDO_NO_HABILITADO,
                        "El partido está " + match.status() + ": no se controla el ingreso");

        // Los rechazos también se auditan: varios intentos con el mismo QR pueden ser un fraude
        auditService.record(null, response.valid() ? AuditAction.ENTRADA_VALIDADA : AuditAction.ENTRADA_RECHAZADA,
                "TICKET", response.ticketPublicId() != null ? response.ticketPublicId().toString() : "QR desconocido",
                response.valid() ? AuditResult.EXITO : AuditResult.FALLO,
                "Partido " + matchId + ": " + response.result() + " · " + response.message());
        return response;
    }
}
