package ec.edu.uce.tickets.match.service;

import ec.edu.uce.tickets.match.dto.StadiumResponse;
import ec.edu.uce.tickets.match.repository.StadiumRepository;
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
}
