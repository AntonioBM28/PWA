package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
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
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClienteController {

    private static final String MENSAJE_ID = "El id del cliente debe ser un numero positivo";

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

    @Operation(summary = "Consultar clientes (EJECUTIVO)",
            description = "Sin filtros devuelve todos los clientes (paginados). Los filtros son opcionales y se combinan")
    @PreAuthorize(Permisos.EJECUTIVO)
    @GetMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientesResponse> consultarClientes(@Valid @ParameterObject ClienteFiltro filtro) {
        return new ResponseEntity<>(clienteService.consultarClientes(filtro), HttpStatus.OK);
    }

    @Operation(summary = "Consultar un cliente por id",
            description = "Incluye su domicilio y sus cuentas. Un CLIENTE solo puede consultar sus propios datos")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_DEL_ID)
    @GetMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteDetalleResponse> consultarCliente(
            @PathVariable @Positive(message = MENSAJE_ID) Integer id) {
        return new ResponseEntity<>(clienteService.consultarCliente(id), HttpStatus.OK);
    }

    @Operation(summary = "Actualizar parcialmente un cliente",
            description = "Solo cambia los campos enviados. activo=false da de baja logica al cliente (sus cuentas y "
                    + "su usuario quedan inactivos); activo=true lo reactiva. CURP, RFC y numero de cuenta no se pueden "
                    + "modificar. Un CLIENTE solo puede modificar sus propios datos y no puede cambiar 'activo'")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_SIN_CAMBIO_DE_ESTATUS)
    @PatchMapping(value = "/clientes/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteDetalleResponse> actualizarCliente(
            @PathVariable @Positive(message = MENSAJE_ID) Integer id,
            @Valid @RequestBody ActualizaClienteRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(id, request), HttpStatus.OK);
    }
}
