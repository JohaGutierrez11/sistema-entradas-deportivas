package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByPublicId(UUID publicId);

    /**
     * Busca la entrada por su QR y la BLOQUEA (SELECT ... FOR UPDATE): si el mismo QR se presenta en dos
     * puertas a la vez, la segunda espera a la primera y ve la entrada ya UTILIZADA (HU-47).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t JOIN FETCH t.reservation WHERE t.qrCode = :qrCode")
    Optional<Ticket> findByQrCodeForUpdate(@Param("qrCode") String qrCode);
}