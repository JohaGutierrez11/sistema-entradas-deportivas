package ec.edu.uce.tickets.reservation.repository;

import ec.edu.uce.tickets.reservation.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}