package ec.edu.uce.tickets.auth.repository;

import ec.edu.uce.tickets.auth.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByCedula(String cedula);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Client> findByUserId(Long userId);
}