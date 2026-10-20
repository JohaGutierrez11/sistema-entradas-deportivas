package ec.edu.uce.tickets.auth.repository;

import ec.edu.uce.tickets.auth.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByCedula(String cedula);
    boolean existsByEmailIgnoreCase(String email);
}