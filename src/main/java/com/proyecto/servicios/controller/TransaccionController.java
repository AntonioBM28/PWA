package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionRequest;
import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionResponse;
import com.proyecto.servicios.model.transaccion.ConsultaTransaccionRequest;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.OperacionFiltro;
import com.proyecto.servicios.model.transaccion.OperacionesResponse;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.transaccion.AutorizacionTransaccionService;
import com.proyecto.servicios.service.transaccion.OperacionService;
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
    private final OperacionService operacionService;
    private final AutorizacionTransaccionService autorizacionService;

    public TransaccionController(TransaccionService transaccionService, OperacionService operacionService,
                                 AutorizacionTransaccionService autorizacionService) {
        this.transaccionService = transaccionService;
        this.operacionService = operacionService;
        this.autorizacionService = autorizacionService;
    }

    @Operation(summary = "Historial de operaciones",
            description = "EJECUTIVO: bitacora de compras y pagos de todos los clientes (filtros por cliente, nombre, "
                    + "cuenta, estado, producto y fechas). CLIENTE: solo su historial de compras. Paginado, de la mas "
                    + "reciente a la mas antigua")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/transacciones/historial", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OperacionesResponse> historial(@Valid @RequestBody OperacionFiltro filtro,
                                                         JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(operacionService.consultar(filtro, authentication));
    }

    @Operation(summary = "Confirmar una compra con el NIP (paso 1 de 2)",
            description = "Solo CLIENTE, con una cuenta propia. Valida el producto, el monto y el NIP, y devuelve el "
                    + "token dinamico de 8 digitos que la app usa automaticamente (no lo muestra); vence en 60 segundos "
                    + "y solo sirve para esa operacion")
    @PreAuthorize(Permisos.CLIENTE_DUENO_DE_LA_CUENTA)
    @PostMapping(value = "/transacciones/autorizacion", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AutorizacionTransaccionResponse> autorizar(
            @Valid @RequestBody AutorizacionTransaccionRequest request, JwtAuthenticationToken authentication) {
        return ResponseEntity.ok(autorizacionService.autorizar(request, authentication));
    }

    @Operation(summary = "Comprar un producto o pagar un servicio con GestoPago (paso 2 de 2)",
            description = "Requiere Idempotency-Key y el token dinamico del paso 1. Solo CLIENTE con una cuenta propia; "
                    + "el UPC se genera internamente. Una operacion rechazada responde 200 con estado RECHAZADA y los "
                    + "datos del ticket")
    @PreAuthorize(Permisos.CLIENTE_DUENO_DE_LA_CUENTA)
    @PostMapping(value = "/transacciones", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransaccionResponse> comprar(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CrearTransaccionRequest request,
            JwtAuthenticationToken authentication) {
        autorizacionService.consumir(request, idempotencyKey, authentication);
        TransaccionResponse response = transaccionService.comprar(request.getNumeroCuenta(), idempotencyKey, request,
                authentication);
        HttpStatus status = switch (response.getEstado()) {
            case APROBADA -> HttpStatus.CREATED;
            case RECHAZADA -> HttpStatus.OK;
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
