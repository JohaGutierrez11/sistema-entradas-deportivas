package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** De los asientos pedidos, devuelve solo los que existen y pertenecen a ese estadio. */
    @Query("SELECT s.id FROM Seat s WHERE s.id IN :seatIds AND s.seatRow.section.stadium.id = :stadiumId")
    List<Long> findValidSeatIds(@Param("stadiumId") Long stadiumId,
                                @Param("seatIds") Collection<Long> seatIds);
}