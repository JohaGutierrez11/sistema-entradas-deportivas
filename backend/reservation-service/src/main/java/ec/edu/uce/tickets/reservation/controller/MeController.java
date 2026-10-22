package ec.edu.uce.tickets.reservation.controller;

import ec.edu.uce.tickets.reservation.dto.MeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sesión")
@RestController
@RequestMapping("/reservation-api/v1")
public class MeController {

    /** Muestra lo que reservation-service lee del token, sin consultar a auth-service. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        Long clientId = jwt.getClaim("clientId") instanceof Number number ? number.longValue() : null;
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("role"), clientId, jwt.getExpiresAt());
    }
}