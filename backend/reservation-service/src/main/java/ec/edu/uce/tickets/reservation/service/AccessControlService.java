package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditResult;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.client.MatchInfo;
import ec.edu.uce.tickets.reservation.client.MatchServiceClient;
import ec.edu.uce.tickets.reservation.dto.MatchSummary;
import ec.edu.uce.tickets.reservation.dto.TicketValidationResponse;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

/**
 * Control de acceso en la puerta del escenario (HU-47): valida el QR de una entrada y la marca como UTILIZADA.
 * SIN @Transactional: primero se consulta a match-service y después se abre la transacción (como al reservar).
 */
@Service
public class AccessControlService {

    /** Se controla el ingreso antes del inicio (se abren puertas) y durante el partido. */
    private static final Set<String> OPEN_STATUSES = Set.of("PROGRAMADO", "EN_CURSO");
    private static final DateTimeFormatter MATCH_DATE =
            DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-EC"));

    private final MatchServiceClient matchServiceClient;
    private final TicketService ticketService;
    private final AuditService auditService;
    private final MatchLookup matchLookup;

    public AccessControlService(MatchServiceClient matchServiceClient, TicketService ticketService,
                                AuditService auditService, MatchLookup matchLookup) {
        this.matchServiceClient = matchServiceClient;
        this.ticketService = ticketService;
        this.auditService = auditService;
        this.matchLookup = matchLookup;
    }

    public TicketValidationResponse validate(String qrCode, Long matchId) {
        MatchInfo match = matchServiceClient.getMatch(matchId);   // 404 o 503
        TicketValidationResponse response = OPEN_STATUSES.contains(match.status())
                ? ticketService.use(qrCode, matchId)
                : TicketValidationResponse.rejected(TicketValidationResponse.Result.PARTIDO_NO_HABILITADO,
                        "El partido está " + match.status() + ": no se controla el ingreso");

        // OTRO_PARTIDO: el nombre del partido de la entrada vive en match_db → se pide a match-service,
        // DESPUÉS de cerrar la transacción. Si no responde, el mensaje se queda con el número (degradación).
        if (response.result() == TicketValidationResponse.Result.OTRO_PARTIDO && response.ticketMatchId() != null) {
            MatchSummary ticketMatch = matchLookup.findOrNull(response.ticketMatchId());
            if (ticketMatch != null) {
                response = response.withMessage("La entrada es de otro partido: " + ticketMatch.name()
                        + " (" + ticketMatch.matchDate().format(MATCH_DATE) + ", " + ticketMatch.matchTime() + ")");
            }
        }

        // Los rechazos también se auditan: varios intentos con el mismo QR pueden ser un fraude
        auditService.record(null, response.valid() ? AuditAction.ENTRADA_VALIDADA : AuditAction.ENTRADA_RECHAZADA,
                "TICKET", response.ticketPublicId() != null ? response.ticketPublicId().toString() : "QR desconocido",
                response.valid() ? AuditResult.EXITO : AuditResult.FALLO,
                "Partido " + matchId + ": " + response.result() + " · " + response.message());
        return response;
    }
}
