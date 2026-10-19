package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.StadiumRequest;
import ec.edu.uce.tickets.match.dto.StadiumResponse;
import ec.edu.uce.tickets.match.service.StadiumService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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

    @GetMapping("/{stadiumId}")
    public StadiumResponse findById(@PathVariable Long stadiumId){
        return stadiumService.findById(stadiumId);
    }

    @PostMapping
    public ResponseEntity<StadiumResponse> create(@Valid @RequestBody StadiumRequest request){
        StadiumResponse created = stadiumService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
