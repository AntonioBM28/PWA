package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.service.CodigoPostalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodigoPostalController {

    private final CodigoPostalService codigoPostalService;

    public CodigoPostalController(CodigoPostalService codigoPostalService) {
        this.codigoPostalService = codigoPostalService;
    }

    @SecurityRequirements
    @GetMapping(value = "/codigos-postales/{codigoPostal}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CodigoPostalResponse> consultarCodigoPostal(
            @PathVariable
            @Pattern(regexp = "^[0-9]{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
            String codigoPostal) {
        return new ResponseEntity<>(codigoPostalService.consultarCodigoPostal(codigoPostal), HttpStatus.OK);
    }
}
