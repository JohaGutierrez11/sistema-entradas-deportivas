package ec.edu.uce.tickets.auth.controller;

import ec.edu.uce.tickets.auth.dto.RegisterRequest;
import ec.edu.uce.tickets.auth.dto.RegisterResponse;
import ec.edu.uce.tickets.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/auth-api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
}