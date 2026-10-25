package ec.edu.uce.tickets.auth.controller;

import ec.edu.uce.tickets.auth.dto.ClientSummaryResponse;
import ec.edu.uce.tickets.auth.service.ClientLookupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de clientes para el administrador (HU-44). Solo ADMIN: la regla está en SecurityConfig. */
@Tag(name = "Administración de usuarios y roles (ADMIN)")
@RestController
@RequestMapping("/auth-api/v1/clients")
public class ClientController {

    private final ClientLookupService clientLookupService;

    public ClientController(ClientLookupService clientLookupService) {
        this.clientLookupService = clientLookupService;
    }

    /** GET /auth-api/v1/clients?ids=1,2,4 → los que existan; los ids inexistentes simplemente no vienen. */
    @GetMapping
    public List<ClientSummaryResponse> findByIds(@RequestParam List<Long> ids) {
        return clientLookupService.findByIds(ids);
    }
}
