package ec.edu.uce.tickets.match.controller;

import ec.edu.uce.tickets.match.dto.MatchOccupancyResponse;
import ec.edu.uce.tickets.match.dto.OccupancyRequest;
import ec.edu.uce.tickets.match.dto.SeatValidationRequest;
import ec.edu.uce.tickets.match.dto.SeatValidationResponse;
import ec.edu.uce.tickets.match.service.InternalSeatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Interno")
@RestController
@RequestMapping("/match-api/v1/internal")
public class InternalController {

    private final InternalSeatService internalSeatService;

    public InternalController(InternalSeatService internalSeatService) {
        this.internalSeatService = internalSeatService;
    }

    @PostMapping("/seats/validate")
    public SeatValidationResponse validateSeats(
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
            @Valid @RequestBody SeatValidationRequest request) {
        internalSeatService.checkInternalKey(internalKey);
        return internalSeatService.validateSeats(request);
    }
    @PostMapping("/matches/{matchId}/occupancy")
    public MatchOccupancyResponse occupancy(
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey,
            @PathVariable Long matchId,
            @Valid @RequestBody OccupancyRequest request) {
        internalSeatService.checkInternalKey(internalKey);
        return internalSeatService.occupancy(matchId, request);
    }
}