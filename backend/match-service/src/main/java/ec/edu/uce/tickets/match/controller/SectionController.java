package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.SectionRequest;
import ec.edu.uce.tickets.match.dto.SectionResponse;
import ec.edu.uce.tickets.match.service.SectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/match-api/v1")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/stadiums/{stadiumId}/sections")
    public List<SectionResponse> findByStadium(@PathVariable Long stadiumId) {
        return sectionService.findByStadium(stadiumId);
    }

    @PostMapping("/stadiums/{stadiumId}/sections")
    public ResponseEntity<SectionResponse> create(@PathVariable Long stadiumId,
                                                  @Valid @RequestBody SectionRequest request) {
        SectionResponse created = sectionService.create(stadiumId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/match-api/v1/sections/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}