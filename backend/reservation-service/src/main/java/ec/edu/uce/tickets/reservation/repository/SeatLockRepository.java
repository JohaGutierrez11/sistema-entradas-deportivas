package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
    List<SeatLock> findByMatchId(Long matchId);
}