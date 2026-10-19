package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.MatchRequest;
import ec.edu.uce.tickets.match.dto.MatchResponse;
import ec.edu.uce.tickets.match.dto.MatchUpdateRequest;
import ec.edu.uce.tickets.match.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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
}