package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.AvailabilityResponse;
import ec.edu.uce.tickets.reservation.service.AvailabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Disponibilidad")
@RestController
@RequestMapping("/reservation-api/v1/matches")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{matchId}/availability")
    public AvailabilityResponse getAvailability(@PathVariable Long matchId) {
        return availabilityService.getAvailability(matchId);
    }
}