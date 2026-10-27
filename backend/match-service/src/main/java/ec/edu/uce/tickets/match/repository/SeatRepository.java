package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.dto.SeatLabelResponse;
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

    /** Cuántos de esos asientos hay en cada sección del estadio. */
    @Query("""
        SELECT s.seatRow.section.id, COUNT(s) FROM Seat s
        WHERE s.id IN :seatIds AND s.seatRow.section.stadium.id = :stadiumId
        GROUP BY s.seatRow.section.id
        """)
    List<Object[]> countBySection(@Param("stadiumId") Long stadiumId,
                                  @Param("seatIds") Collection<Long> seatIds);

    /** Sección, fila y número de cada asiento (una sola consulta con JOIN). */
    @Query("""
        SELECT new ec.edu.uce.tickets.match.dto.SeatLabelResponse(
            s.id, s.seatRow.section.name, s.seatRow.letter, s.seatNumber)
        FROM Seat s WHERE s.id IN :seatIds
        ORDER BY s.id
        """)
    List<SeatLabelResponse> findLabels(@Param("seatIds") Collection<Long> seatIds);
}