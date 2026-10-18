package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.StadiumResponse;
import ec.edu.uce.tickets.match.service.StadiumService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/match-api/v1/stadiums")
public class StadiumController {

    private final StadiumService stadiumService;

    public StadiumController(StadiumService stadiumService){
        this.stadiumService = stadiumService;
    }

    @GetMapping
    public List<StadiumResponse> findAll() {
        return stadiumService.findAll();
    }
}
