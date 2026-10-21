package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByPublicId(UUID publicId);
}