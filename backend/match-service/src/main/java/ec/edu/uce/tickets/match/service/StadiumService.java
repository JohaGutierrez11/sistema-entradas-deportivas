package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.StadiumRequest;
import ec.edu.uce.tickets.match.dto.StadiumResponse;
import ec.edu.uce.tickets.match.entity.Stadium;
import ec.edu.uce.tickets.match.exception.DuplicateResourceException;
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

    public StadiumResponse findById(Long id){
        return stadiumRepository.findById(id)
                .map(StadiumResponse::from)
                .orElseThrow(()-> new ResourceNotFoundException("No existe el estadio con el id "+ id));
    }

    @Transactional
    public StadiumResponse create(StadiumRequest request) {

        String name = request.name().trim();
        if (stadiumRepository.existsByNameIgnoreCase(name)){
            throw new DuplicateResourceException("Ya existe un estadio con el nombre \"" + name + "\"");
        }

        Stadium stadium = new Stadium();
        stadium.setName(request.name().trim());
        stadium.setDescription(request.description());
        stadium.setLocation(request.location().trim());
        stadium.setTotalCapacity(request.totalCapacity());

        Stadium saved = stadiumRepository.save(stadium);
        return StadiumResponse.from(saved);
    }
}
