package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesion",
            description = "Devuelve un token JWT para consumir los endpoints protegidos. En Swagger, copie el token "
                    + "y peguelo en el boton Authorize")
    @SecurityRequirements
    @PostMapping(value = "/auth/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(authService.iniciarSesion(request), HttpStatus.OK);
    }
}
