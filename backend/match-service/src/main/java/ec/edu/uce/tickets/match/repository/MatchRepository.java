package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.SportMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<SportMatch, Long> {
}