package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Reservation;
import ec.edu.uce.tickets.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByCode(String code);

    Optional<Reservation> findByPublicId(UUID publicId);

    List<Reservation> findByMatchIdAndStatusAndExpiresAtLessThanEqual(
            Long matchId, ReservationStatus status, LocalDateTime now);
}