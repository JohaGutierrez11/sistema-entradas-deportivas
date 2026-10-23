package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.SectionBoxRequest;
import ec.edu.uce.tickets.match.dto.SectionRequest;
import ec.edu.uce.tickets.match.dto.SectionResponse;
import ec.edu.uce.tickets.match.dto.StadiumLayoutResponse;
import ec.edu.uce.tickets.match.entity.*;
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
        // 4b. Si viene ubicada, el recuadro no puede pisar la cancha ni otra sección
        SectionBox box = request.box() != null ? request.box().toBox() : null;
        if (box != null) {
            validateBox(stadiumId, null, box);
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
        section.setBox(box);

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

    /** Ubica o mueve una sección en el plano (HU-38). */
    @Transactional
    public SectionResponse updateBox(Long sectionId, SectionBoxRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la sección con el id " + sectionId));

        SectionBox box = request.toBox();
        validateBox(section.getStadium().getId(), sectionId, box);   // se ignora a sí misma

        section.setBox(box);   // dirty checking: se guarda al terminar la transacción
        return SectionResponse.from(section);
    }

    /** El recuadro no puede pisar la cancha ni a otra sección del mismo estadio → 422. */
    private void validateBox(Long stadiumId, Long sectionIdToIgnore, SectionBox box) {
        if (box.overlaps(SectionBox.FIELD)) {
            throw new BusinessRuleException("El recuadro se superpone con la cancha (x 28-72, y 25-75)");
        }
        for (Section other : sectionRepository.findByStadiumIdOrderByIdAsc(stadiumId)) {
            boolean isItself = other.getId().equals(sectionIdToIgnore);
            if (!isItself && other.getBox() != null && box.overlaps(other.getBox())) {
                throw new BusinessRuleException("El recuadro se superpone con la sección \"" + other.getName() + "\"");
            }
        }
    }

    /** Estructura completa del estadio para el mapa de asientos (HU-38). */
    @Transactional(readOnly = true)
    public StadiumLayoutResponse getLayout(Long stadiumId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el estadio con el id " + stadiumId));
        // readOnly: las filas y los asientos son LAZY y se cargan aquí, con la sesión abierta
        return StadiumLayoutResponse.from(stadium, sectionRepository.findByStadiumIdOrderByIdAsc(stadiumId));
    }
}
