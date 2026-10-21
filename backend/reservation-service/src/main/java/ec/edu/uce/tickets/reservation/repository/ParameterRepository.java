package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Parameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParameterRepository extends JpaRepository<Parameter, Long> {
    Optional<Parameter> findByCode(String code);
}