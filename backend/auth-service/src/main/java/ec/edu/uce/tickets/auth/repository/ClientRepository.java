package ec.edu.uce.tickets.auth.repository;

import ec.edu.uce.tickets.auth.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByCedula(String cedula);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Client> findByUserId(Long userId);

    @Query("SELECT c FROM Client c JOIN FETCH c.user WHERE c.id IN :ids ORDER BY c.id")
    List<Client> findWithUserByIdIn(@Param("ids") List<Long> ids);
}