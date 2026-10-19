package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query(value = "SELECT COALESCE(SUM(capacity),0) FROM section WHERE stadium_id = :stadiumId", nativeQuery = true)
    int sumSectionCapacity(@Param("stadiumId") Long stadiumId);

    @Query(value = "SELECT COUNT(*) FROM section WHERE stadium_id = :stadiumId", nativeQuery = true)
    long countSections(@Param("stadiumId") Long stadiumId);

    @Query(value = "SELECT COUNT(*) FROM sport_match WHERE stadium_id = :stadiumId", nativeQuery = true)
    long countMatches(@Param("stadiumId") Long stadiumId);

}
