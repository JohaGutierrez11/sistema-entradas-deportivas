package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    /**
     * ¿Ya hay un estadio con ese nombre, sin contar tildes, mayúsculas ni espacios de más?
     * Usa la misma función name_key que el índice único uk_stadium_name: una sola regla en la BD.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM stadium WHERE name_key(name) = name_key(:name))", nativeQuery = true)
    boolean existsByNameKey(@Param("name") String name);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM stadium WHERE name_key(name) = name_key(:name) AND id <> :id)",
            nativeQuery = true)
    boolean existsByNameKeyAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query(value = "SELECT COALESCE(SUM(capacity),0) FROM section WHERE stadium_id = :stadiumId", nativeQuery = true)
    int sumSectionCapacity(@Param("stadiumId") Long stadiumId);

    @Query(value = "SELECT COUNT(*) FROM section WHERE stadium_id = :stadiumId", nativeQuery = true)
    long countSections(@Param("stadiumId") Long stadiumId);

    @Query(value = "SELECT COUNT(*) FROM sport_match WHERE stadium_id = :stadiumId", nativeQuery = true)
    long countMatches(@Param("stadiumId") Long stadiumId);

}
