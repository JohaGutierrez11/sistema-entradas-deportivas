package ec.edu.uce.tickets.reservation.service;

import ec.edu.uce.tickets.reservation.repository.ParameterRepository;
import org.springframework.stereotype.Service;

@Service
public class ParameterService {

    private final ParameterRepository parameterRepository;

    public ParameterService(ParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    public int getInt(String code) {
        String value = parameterRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + code))
                .getValue();
        return Integer.parseInt(value);
    }
}