package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    boolean existsByCode(String code);

    Optional<Reservation> findByPublicId(UUID publicId);

    List<Reservation> findByMatchIdAndStatusAndExpiresAtLessThanEqual(
            Long matchId, ReservationStatus status, LocalDateTime now);
    /** Lee la reserva y la BLOQUEA hasta que termine la transacción (SELECT ... FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.publicId = :publicId")
    Optional<Reservation> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    /** Todas las reservas pendientes ya vencidas, BLOQUEADAS para que nadie las confirme mientras tanto. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.expiresAt <= :now")
    List<Reservation> findExpiredForUpdate(@Param("status") ReservationStatus status,
                                           @Param("now") LocalDateTime now);

    List<Reservation> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Partidos que todavía tienen reservas activas (HU-46): son los únicos que vale la pena revisar. */
    @Query("SELECT DISTINCT r.matchId FROM Reservation r WHERE r.status IN :statuses")
    List<Long> findMatchIdsWithStatusIn(@Param("statuses") List<ReservationStatus> statuses);

    /** Reservas activas de un partido, BLOQUEADAS: nadie las confirma ni cancela mientras se anulan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.matchId = :matchId AND r.status IN :statuses")
    List<Reservation> findByMatchIdAndStatusInForUpdate(@Param("matchId") Long matchId,
                                                        @Param("statuses") List<ReservationStatus> statuses);
}