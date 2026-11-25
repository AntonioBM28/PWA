package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.transaccion.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.transaccion.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transacciones con GestoPago. La cuenta, el producto y el id de la transaccion se reciben en el
 * cuerpo JSON. Idempotency-Key viaja en un header porque es metadato de la peticion (evita cobrar
 * dos veces si el cliente reintenta), no un dato de la operacion.
 */
@RestController
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @Operation(summary = "Comprar un producto o pagar un servicio con GestoPago",
            description = "Requiere Idempotency-Key. El cliente solo puede operar su propia cuenta; el UPC se genera internamente.")
    @PreAuthorize(Permisos.EJECUTIVO_O_DUENO_DE_LA_CUENTA)
    @PostMapping(value = "/transacciones", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransaccionResponse> comprar(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CrearTransaccionRequest request,
            JwtAuthenticationToken authentication) {
        TransaccionResponse response = transaccionService.comprar(request.getNumeroCuenta(), idempotencyKey, request,
                authentication);
        HttpStatus status = switch (response.getEstado()) {
            case APROBADA -> HttpStatus.CREATED;
            case RECHAZADA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDIENTE, CONCILIACION -> HttpStatus.ACCEPTED;
        };
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Consultar el estado de una transacción")
    @PostMapping(value = "/transacciones/consulta", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransaccionResponse> consultar(@Valid @RequestBody ConsultaTransaccionRequest request,
                                                          JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(transaccionService.consultar(request.getId(), authentication));
    }
}
