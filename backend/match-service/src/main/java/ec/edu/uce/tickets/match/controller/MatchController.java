package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.MatchRequest;
import ec.edu.uce.tickets.match.dto.MatchResponse;
import ec.edu.uce.tickets.match.dto.MatchUpdateRequest;
import ec.edu.uce.tickets.match.entity.MatchStatus;
import ec.edu.uce.tickets.match.service.MatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Partidos")
@RestController
@RequestMapping("/match-api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> create(@Valid @RequestBody MatchRequest request) {
        MatchResponse created = matchService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{matchId}")
    public MatchResponse findById(@PathVariable Long matchId) {
        return matchService.findById(matchId);
    }

    @PutMapping("/{matchId}")
    public MatchResponse update(@PathVariable Long matchId,
                                @Valid @RequestBody MatchUpdateRequest request) {
        return matchService.update(matchId, request);
    }

    @GetMapping
    public List<MatchResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @Size(min = 2, max = 100) String name,
            @RequestParam(required = false) @Min(1) Long stadiumId,
            @RequestParam(required = false) MatchStatus status) {
        return matchService.search(date, name, stadiumId, status);
    }
}