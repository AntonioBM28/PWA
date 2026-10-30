package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.transaccion.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class TransaccionController {
    private static final String CUENTA = "^[0-9]{10}$";
    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @Operation(summary = "Comprar un producto o pagar un servicio con GestoPago",
            description = "Requiere Idempotency-Key. El cliente solo puede operar su propia cuenta; el UPC se genera internamente.")
    @PreAuthorize(Permisos.EJECUTIVO_O_DUENO_DE_LA_CUENTA)
    @PostMapping(value = "/cuentas/{numeroCuenta}/transacciones", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransaccionResponse> comprar(
            @PathVariable @Pattern(regexp = CUENTA) String numeroCuenta,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CrearTransaccionRequest request,
            JwtAuthenticationToken authentication) {
        TransaccionResponse response = transaccionService.comprar(numeroCuenta, idempotencyKey, request, authentication);
        HttpStatus status = switch (response.getEstado()) {
            case APROBADA -> HttpStatus.CREATED;
            case RECHAZADA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDIENTE, CONCILIACION -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Consultar el estado de una transacción")
    @GetMapping(value = "/transacciones/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransaccionResponse> consultar(@PathVariable UUID id,
                                                          JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(transaccionService.consultar(id, authentication));
    }
}
