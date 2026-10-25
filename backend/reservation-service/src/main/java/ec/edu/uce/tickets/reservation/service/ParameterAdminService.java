package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.audit.AuditAction;
import ec.edu.uce.tickets.reservation.audit.AuditService;
import ec.edu.uce.tickets.reservation.dto.ParameterResponse;
import ec.edu.uce.tickets.reservation.entity.Parameter;
import ec.edu.uce.tickets.reservation.exception.BusinessRuleException;
import ec.edu.uce.tickets.reservation.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.reservation.repository.ParameterRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Configuración del sistema por el administrador (HU-43): ver y cambiar los parámetros de la tabla parameter.
 *
 * Cada parámetro editable tiene un rango: un valor fuera de él rompería una regla de negocio
 * (por ejemplo, 0 entradas por partido o una retención de 0 minutos). Los cambios aplican a las
 * reservas NUEVAS; las existentes conservan lo que tenían. RESERVATION_CODE_PREFIX no se edita:
 * cambiarlo mezclaría formatos de código en el historial.
 */
@Service
public class ParameterAdminService {

    /** Rango permitido por parámetro editable. */
    record Rule(int min, int max, String unit) {
    }

    private static final Map<String, Rule> RULES = Map.of(
            "MAX_TICKETS_PER_MATCH", new Rule(1, 10, "entradas"),
            "HOLD_MINUTES", new Rule(1, 30, "minutos"),
            "SALES_CLOSE_OFFSET_MINUTES", new Rule(-1440, 120, "minutos respecto del inicio"),
            "EXPIRATION_JOB_SECONDS", new Rule(10, 600, "segundos"),
            "AVAILABILITY_REFRESH_SECONDS", new Rule(2, 60, "segundos"));

    private final ParameterRepository parameterRepository;
    private final AuditService auditService;

    public ParameterAdminService(ParameterRepository parameterRepository, AuditService auditService) {
        this.parameterRepository = parameterRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ParameterResponse> findAll() {
        return parameterRepository.findAll(Sort.by("id")).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ParameterResponse update(String code, String rawValue) {
        Parameter parameter = parameterRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el parámetro " + code));
        Rule rule = RULES.get(code);
        if (rule == null) {
            throw new BusinessRuleException("El parámetro " + code + " no se puede modificar desde la aplicación");
        }

        int value;
        try {
            value = Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El valor de " + code + " debe ser un número entero");
        }
        if (value < rule.min() || value > rule.max()) {
            throw new BusinessRuleException("El valor de " + code + " debe estar entre " + rule.min()
                    + " y " + rule.max() + " " + rule.unit());
        }

        String previous = parameter.getValue();
        parameter.setValue(String.valueOf(value));   // dirty checking: se guarda al cerrar la transacción
        auditService.success(AuditAction.PARAMETRO_ACTUALIZADO, "PARAMETRO", code, previous + " → " + value);
        return toResponse(parameter);
    }

    private ParameterResponse toResponse(Parameter parameter) {
        Rule rule = RULES.get(parameter.getCode());
        return new ParameterResponse(parameter.getCode(), parameter.getValue(), parameter.getDescription(),
                rule != null, rule != null ? rule.min() : null, rule != null ? rule.max() : null,
                rule != null ? rule.unit() : null);
    }
}
