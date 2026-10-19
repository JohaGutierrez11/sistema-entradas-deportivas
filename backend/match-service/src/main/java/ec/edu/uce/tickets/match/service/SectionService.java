package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.SectionRequest;
import ec.edu.uce.tickets.match.dto.SectionResponse;
import ec.edu.uce.tickets.match.entity.Seat;
import ec.edu.uce.tickets.match.entity.SeatRow;
import ec.edu.uce.tickets.match.entity.Section;
import ec.edu.uce.tickets.match.entity.Stadium;
import ec.edu.uce.tickets.match.exception.BusinessRuleException;
import ec.edu.uce.tickets.match.exception.DuplicateResourceException;
import ec.edu.uce.tickets.match.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.match.repository.SectionRepository;
import ec.edu.uce.tickets.match.repository.StadiumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final StadiumRepository stadiumRepository;

    public SectionService(SectionRepository sectionRepository, StadiumRepository stadiumRepository){
        this.sectionRepository = sectionRepository;
        this.stadiumRepository = stadiumRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findByStadium(Long stadiumId){
        if (!stadiumRepository.existsById(stadiumId)) {
            throw new ResourceNotFoundException("No existe el estadio con el id " + stadiumId);
        }

        return sectionRepository.findByStadiumIdOrderByIdAsc(stadiumId).stream()
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional
    public SectionResponse create(Long stadiumId, SectionRequest request){

        // 1. El estadio debe existir
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(()->new ResourceNotFoundException("No existe el estadio con el id " + stadiumId));

        //2. El nombre no puede repetirse dentro del mismo estadio
        String name = request.name().trim();
        if (sectionRepository.existsByStadiumIdAndNameIgnoreCase(stadiumId, name)){
            throw new DuplicateResourceException("Ya existe una sección con el nombre \"" + name + "\" en este estadio");
        }

        // 3. Capacidad de la nueva sección
        int capacity = request.rowCount() * request.seatsPerRow();

        // 4. La suma de secciones no puede superar la capacidad del estadio
        long newTotal = sectionRepository.sumCapacityByStadiumId(stadiumId) + capacity;
        if (newTotal > stadium.getTotalCapacity()){
            throw new BusinessRuleException("La capacidad de las secciones (" + newTotal
                    + ") superaría la capacidad total del estadio (" + stadium.getTotalCapacity() + ")");
        }

        //5. Crear la seccion
        Section section = new Section();
        section.setStadium(stadium);
        section.setName(name);
        section.setDescription(request.description());
        section.setSectionType(request.sectionType());
        section.setRowCount(request.rowCount());
        section.setSeatsPerRow(request.seatsPerRow());
        section.setCapacity(capacity);

        //6. Generar filas (A,B,C....) y dentro de cada uno, de los asientos 1. .N
        for (int i = 0; i < request.rowCount(); i++){
            SeatRow row = new SeatRow();
            row.setLetter(String.valueOf((char) ('A'+i)));

            for (int n = 1; n <= request.seatsPerRow(); n++){
                Seat seat = new Seat();
                seat.setSeatNumber(n);
                row.addSeat(seat);
            }
            section.addRow(row);
        }

        //7. Un solo save: el cascade guarda filas y asientos
        Section saved = sectionRepository.save(section);
        return SectionResponse.from(saved);

    }
}
