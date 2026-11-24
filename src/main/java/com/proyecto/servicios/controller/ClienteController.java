package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteDetalleResponse;
import com.proyecto.servicios.model.onboarding.ClienteFiltro;
import com.proyecto.servicios.model.onboarding.ClienteIdRequest;
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

/**
 * Clientes. Todos los datos, incluidos los criterios de busqueda y el id del cliente, se reciben
 * en el cuerpo JSON (Request DTO) y se validan con @Valid.
 */
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

    @Operation(summary = "Consultar clientes (EJECUTIVO)",
            description = "Un solo endpoint de busqueda: sin filtros devuelve todos los clientes (paginados); los "
                    + "filtros enviados en el cuerpo son opcionales y se combinan")
    @PreAuthorize(Permisos.EJECUTIVO)
    @PostMapping(value = "/clientes/consulta", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientesResponse> consultarClientes(@Valid @RequestBody ClienteFiltro filtro) {
        return new ResponseEntity<>(clienteService.consultarClientes(filtro), HttpStatus.OK);
    }

    @Operation(summary = "Consultar el detalle de un cliente",
            description = "Incluye su domicilio y sus cuentas. Un CLIENTE solo puede consultar sus propios datos")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_DEL_ID)
    @PostMapping(value = "/clientes/detalle", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteDetalleResponse> consultarCliente(@Valid @RequestBody ClienteIdRequest request) {
        return new ResponseEntity<>(clienteService.consultarCliente(request.getId()), HttpStatus.OK);
    }

    @Operation(summary = "Actualizar parcialmente un cliente",
            description = "El campo id indica el cliente. Solo cambian los demas campos enviados. activo=false da de "
                    + "baja logica al cliente (sus cuentas y su usuario quedan inactivos); activo=true lo reactiva. "
                    + "CURP, RFC y numero de cuenta no se pueden modificar. Un CLIENTE solo puede modificar sus "
                    + "propios datos y no puede cambiar 'activo'")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_SIN_CAMBIO_DE_ESTATUS)
    @PatchMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteDetalleResponse> actualizarCliente(@Valid @RequestBody ActualizaClienteRequest request) {
        return new ResponseEntity<>(clienteService.actualizarCliente(request.getId(), request), HttpStatus.OK);
    }
}
