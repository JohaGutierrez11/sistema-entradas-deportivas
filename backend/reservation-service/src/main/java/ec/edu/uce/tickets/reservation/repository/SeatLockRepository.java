package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.SeatLock;
import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
    @Query("""
        SELECT l FROM SeatLock l JOIN l.reservation r
        WHERE l.matchId = :matchId
          AND (l.status = :reserved OR r.expiresAt > :now)
        """)
    List<SeatLock> findOccupiedSeats(@Param("matchId") Long matchId,
                                     @Param("reserved") SeatLockStatus reserved,
                                     @Param("now") LocalDateTime now);
}