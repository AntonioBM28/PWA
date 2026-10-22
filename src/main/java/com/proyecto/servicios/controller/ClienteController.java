package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ActualizaClienteConIdRequest;
import com.proyecto.servicios.model.onboarding.ClienteDetalleResponse;
import com.proyecto.servicios.model.onboarding.ClienteFiltro;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClientesResponse;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Operation(summary = "Registrar un cliente",
            description = "Crea el cliente con su domicilio, una cuenta ACTIVA con el saldo inicial y su usuario de acceso")
    @SecurityRequirements
    @PostMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegistroClienteResponse> registrarCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.registrarCliente(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar clientes",
            description = "Los filtros opcionales se combinan. EJECUTIVO puede buscar clientes; CLIENTE debe enviar su propio correo")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_DEL_CORREO)
    @PostMapping(value = "/clientes/busqueda", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientesResponse> consultarClientes(@Valid @RequestBody ClienteFiltro filtro) {
        return new ResponseEntity<>(clienteService.consultarClientes(filtro), HttpStatus.OK);
    }

    @Operation(summary = "Actualizar parcialmente un cliente",
            description = "Recibe clienteId y cambios en el cuerpo. activo=false da de baja logica al cliente (sus cuentas y "
                    + "su usuario quedan inactivos); activo=true lo reactiva. CURP, RFC y numero de cuenta no se pueden "
                    + "modificar. Un CLIENTE solo puede modificar sus propios datos y no puede cambiar 'activo'")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_SIN_CAMBIO_DE_ESTATUS)
    @PatchMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteDetalleResponse> actualizarCliente(@Valid @RequestBody ActualizaClienteConIdRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(request.getClienteId(), request.getCambios()), HttpStatus.OK);
    }
}
