package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.MatchStatus;
import ec.edu.uce.tickets.match.entity.SportMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface MatchRepository extends JpaRepository<SportMatch, Long>,
        JpaSpecificationExecutor<SportMatch> {

    /**
     * Pasa de {@code from} a {@code to} los partidos cuya fecha y hora ya llegaron a {@code date} {@code time}.
     * Fecha y hora están en dos columnas, por eso la comparación es "día anterior, o mismo día y hora ≤".
     * Un solo UPDATE en la base: no se cargan los partidos uno por uno.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SportMatch m SET m.status = :to
            WHERE m.status = :from
              AND (m.matchDate < :date OR (m.matchDate = :date AND m.matchTime <= :time))
            """)
    int moveStartedBefore(@Param("from") MatchStatus from, @Param("to") MatchStatus to,
                          @Param("date") LocalDate date, @Param("time") LocalTime time);
}
