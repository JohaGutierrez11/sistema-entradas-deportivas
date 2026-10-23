package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.SectionBoxRequest;
import ec.edu.uce.tickets.match.dto.SectionRequest;
import ec.edu.uce.tickets.match.dto.SectionResponse;
import ec.edu.uce.tickets.match.dto.StadiumLayoutResponse;
import ec.edu.uce.tickets.match.service.SectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Secciones")
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

    @PutMapping("/sections/{sectionId}/box")
    public SectionResponse updateBox(@PathVariable Long sectionId,
                                     @Valid @RequestBody SectionBoxRequest request) {
        return sectionService.updateBox(sectionId, request);
    }

    @GetMapping("/stadiums/{stadiumId}/layout")
    public StadiumLayoutResponse layout(@PathVariable Long stadiumId) {
        return sectionService.getLayout(stadiumId);
    }
}