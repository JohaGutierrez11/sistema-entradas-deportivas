package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.ParameterResponse;
import ec.edu.uce.tickets.reservation.dto.UpdateParameterRequest;
import ec.edu.uce.tickets.reservation.service.ParameterAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Parámetros del sistema (HU-43). Solo ADMIN: la regla está en SecurityConfig. */
@Tag(name = "Configuración")
@RestController
@RequestMapping("/reservation-api/v1/parameters")
public class ParameterController {

    private final ParameterAdminService parameterAdminService;

    public ParameterController(ParameterAdminService parameterAdminService) {
        this.parameterAdminService = parameterAdminService;
    }

    @GetMapping
    public List<ParameterResponse> findAll() {
        return parameterAdminService.findAll();
    }

    @PutMapping("/{code}")
    public ParameterResponse update(@PathVariable String code, @Valid @RequestBody UpdateParameterRequest request) {
        return parameterAdminService.update(code, request.value());
    }
}
