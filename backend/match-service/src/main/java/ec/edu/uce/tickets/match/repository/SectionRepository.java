package ec.edu.uce.tickets.match.repository;

import ec.edu.uce.tickets.match.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByStadiumIdOrderByIdAsc(Long stadiumId);

    boolean existsByStadiumIdAndNameIgnoreCase(Long stadiumId, String name);

    @Query("SELECT COALESCE(SUM(s.capacity), 0) FROM Section s WHERE s.stadium.id = :stadiumId")
    long sumCapacityByStadiumId(@Param("stadiumId") Long stadiumId);
}