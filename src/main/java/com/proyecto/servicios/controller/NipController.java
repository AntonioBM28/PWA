package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.transaccion.NipEstadoResponse;
import com.proyecto.servicios.model.transaccion.NipRequest;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.transaccion.NipService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** NIP de transacciones del CLIENTE: se configura una vez y autoriza cada compra. */
@RestController
public class NipController {

    private final NipService nipService;

    public NipController(NipService nipService) {
        this.nipService = nipService;
    }

    @Operation(summary = "Consultar si el cliente ya configuro su NIP")
    @PreAuthorize(Permisos.CLIENTE)
    @GetMapping(value = "/nip", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NipEstadoResponse> estado(JwtAuthenticationToken autenticacion) {
        NipService.EstadoNip estado = nipService.estado(usuarioId(autenticacion));
        NipEstadoResponse response = new NipEstadoResponse();
        response.setCodigo(0);
        response.setMensaje(estado.configurado() ? "NIP configurado" : "Aun no configuras tu NIP");
        response.setConfigurado(estado.configurado());
        response.setBloqueadoHasta(estado.bloqueadoHasta());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Configurar el NIP de transacciones",
            description = "4 digitos, sin digitos repetidos ni consecutivos (1111, 1234). Solo se guarda su hash")
    @PreAuthorize(Permisos.CLIENTE)
    @PostMapping(value = "/nip", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crear(@Valid @RequestBody NipRequest request,
                                                 JwtAuthenticationToken autenticacion) {
        nipService.crear(usuarioId(autenticacion), request.getNip(), request.getConfirmacion());
        return new ResponseEntity<>(exito("Tu NIP quedo configurado"), HttpStatus.CREATED);
    }

    @Operation(summary = "Cambiar el NIP de transacciones", description = "Requiere el NIP actual (nipActual)")
    @PreAuthorize(Permisos.CLIENTE)
    @PatchMapping(value = "/nip", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> cambiar(@Valid @RequestBody NipRequest request,
                                                   JwtAuthenticationToken autenticacion) {
        nipService.cambiar(usuarioId(autenticacion), request.getNipActual(), request.getNip(), request.getConfirmacion());
        return ResponseEntity.ok(exito("Tu NIP se actualizo"));
    }

    private static Integer usuarioId(JwtAuthenticationToken autenticacion) {
        return Integer.valueOf(autenticacion.getToken().getSubject());
    }

    private static GenericResponse exito(String mensaje) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje(mensaje);
        return response;
    }
}
