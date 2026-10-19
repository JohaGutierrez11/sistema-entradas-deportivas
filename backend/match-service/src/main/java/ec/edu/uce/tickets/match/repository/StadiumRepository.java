package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {
    boolean existsByNameIgnoreCase(String name);
}
