package ec.edu.uce.tickets.auth.service;

import ec.edu.uce.tickets.auth.dto.ClientSummaryResponse;
import ec.edu.uce.tickets.auth.exception.BusinessRuleException;
import ec.edu.uce.tickets.auth.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Datos de varios clientes en UNA consulta (HU-44). Lo usa la pantalla de reservas por evento:
 * reservation-service solo conoce el client_id; el nombre vive aquí, en auth_db.
 */
@Service
public class ClientLookupService {

    private static final int MAX_IDS = 100;

    private final ClientRepository clientRepository;

    public ClientLookupService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientSummaryResponse> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = ids.stream().distinct().toList();
        if (distinct.size() > MAX_IDS) {
            throw new BusinessRuleException("Se pueden consultar hasta " + MAX_IDS + " clientes por petición");
        }
        // JOIN FETCH: el usuario viene en la misma consulta (sin N+1)
        return clientRepository.findWithUserByIdIn(distinct).stream().map(ClientSummaryResponse::from).toList();
    }
}
