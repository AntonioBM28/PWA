package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.model.ConsultaCodigoPostalRequest;
import com.proyecto.servicios.service.CodigoPostalService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodigoPostalController {

    private final CodigoPostalService codigoPostalService;

    public CodigoPostalController(CodigoPostalService codigoPostalService) {
        this.codigoPostalService = codigoPostalService;
    }

    @SecurityRequirements
    @PostMapping(value = "/codigos-postales/consulta", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CodigoPostalResponse> consultarCodigoPostal(@Valid @RequestBody ConsultaCodigoPostalRequest request) {
        return new ResponseEntity<>(codigoPostalService.consultarCodigoPostal(request.getCodigoPostal()), HttpStatus.OK);
    }
}
