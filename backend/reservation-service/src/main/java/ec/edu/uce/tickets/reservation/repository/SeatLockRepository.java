package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.SeatLock;
import ec.edu.uce.tickets.reservation.entity.SeatLockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
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
    /** Cuántos asientos activos tiene un cliente en un partido (vendidos o retenidos sin vencer). */
    @Query("""
        SELECT COUNT(l) FROM SeatLock l JOIN l.reservation r
        WHERE r.clientId = :clientId AND l.matchId = :matchId
          AND (l.status = :reserved OR r.expiresAt > :now)
        """)
    long countActiveSeatsOfClient(@Param("clientId") Long clientId,
                                  @Param("matchId") Long matchId,
                                  @Param("reserved") SeatLockStatus reserved,
                                  @Param("now") LocalDateTime now);

    /** De los asientos pedidos, cuáles ya están ocupados. */
    @Query("""
        SELECT l.seatId FROM SeatLock l JOIN l.reservation r
        WHERE l.matchId = :matchId AND l.seatId IN :seatIds
          AND (l.status = :reserved OR r.expiresAt > :now)
        """)
    List<Long> findOccupiedSeatIds(@Param("matchId") Long matchId,
                                   @Param("seatIds") Collection<Long> seatIds,
                                   @Param("reserved") SeatLockStatus reserved,
                                   @Param("now") LocalDateTime now);

    /** Asientos de un partido con cierto estado (para el reporte: los RESERVED = vendidos). */
    @Query("SELECT l.seatId FROM SeatLock l WHERE l.matchId = :matchId AND l.status = :status")
    List<Long> findSeatIds(@Param("matchId") Long matchId, @Param("status") SeatLockStatus status);
}