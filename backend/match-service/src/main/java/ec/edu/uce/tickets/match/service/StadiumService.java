package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.StadiumRequest;
import ec.edu.uce.tickets.match.dto.StadiumResponse;
import ec.edu.uce.tickets.match.entity.Stadium;
import ec.edu.uce.tickets.match.entity.VenueType;
import ec.edu.uce.tickets.match.exception.BusinessRuleException;
import ec.edu.uce.tickets.match.exception.DuplicateResourceException;
import ec.edu.uce.tickets.match.exception.ResourceInUseException;
import ec.edu.uce.tickets.match.exception.ResourceNotFoundException;
import ec.edu.uce.tickets.match.repository.StadiumRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumService {

    private final StadiumRepository stadiumRepository;

    public StadiumService(StadiumRepository stadiumRepository){
        this.stadiumRepository = stadiumRepository;
    }

    public List<StadiumResponse> findAll(){
        return stadiumRepository.findAll(Sort.by("id"))
                .stream()
                .map(StadiumResponse::from)
                .toList();
    }

    private Stadium findStadiumOrThrow(Long id) {
        return stadiumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el estadio con el id " + id));
    }

    public StadiumResponse findById(Long id){
        return StadiumResponse.from(findStadiumOrThrow(id));
    }

    @Transactional
    public StadiumResponse create(StadiumRequest request) {

        String name = NameNormalizer.clean(request.name());
        if (stadiumRepository.existsByNameKey(name)){
            throw new DuplicateResourceException("Ya existe un estadio con el nombre \"" + name + "\"");
        }

        Stadium stadium = new Stadium();
        stadium.setName(name);
        stadium.setDescription(request.description());
        stadium.setLocation(request.location().trim());
        stadium.setTotalCapacity(request.totalCapacity());
        stadium.setVenueType(request.venueType() != null ? request.venueType() : VenueType.ESTADIO);

        Stadium saved = stadiumRepository.save(stadium);
        return StadiumResponse.from(saved);
    }

    @Transactional
    public StadiumResponse update(Long id, StadiumRequest request){

        Stadium stadium = findStadiumOrThrow(id);

        String name = NameNormalizer.clean(request.name());
        if (stadiumRepository.existsByNameKeyAndIdNot(name, id)){
            throw new DuplicateResourceException("Ya existe otro estadio con el nombre \"" + name + "\"");
        }

        int sectionsCapacity = stadiumRepository.sumSectionCapacity(id);
        if (request.totalCapacity() < sectionsCapacity){
            throw new BusinessRuleException("La capacidad total (" + request.totalCapacity()
                    + ") no puede ser menor que la suma de sus secciones (" + sectionsCapacity + ")");
        }

        stadium.setName(name);
        stadium.setDescription(request.description());
        stadium.setLocation(request.location().trim());
        stadium.setTotalCapacity(request.totalCapacity());
        if (request.venueType() != null) {   // al editar, si no se envía, se conserva
            stadium.setVenueType(request.venueType());
        }

        return StadiumResponse.from(stadium);
    }

    @Transactional
    public void delete(Long id){
        Stadium stadium = findStadiumOrThrow(id);

        long sections = stadiumRepository.countSections(id);
        long matches = stadiumRepository.countMatches(id);
        if (sections > 0 || matches > 0){
            throw new ResourceInUseException("El estadio tiene " + sections + " secciones y "
                    + matches + " partidos asociados; no se puede eliminar");
        }

        stadiumRepository.delete(stadium);
    }
}
