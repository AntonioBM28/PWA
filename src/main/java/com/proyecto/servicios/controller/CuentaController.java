package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ActualizaCuentaRequest;
import com.proyecto.servicios.model.onboarding.CrearCuentaRequest;
import com.proyecto.servicios.model.onboarding.CuentaDetalleResponse;
import com.proyecto.servicios.model.onboarding.CuentaFiltro;
import com.proyecto.servicios.model.onboarding.CuentasResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
public class CuentaController {

    private static final String NUMERO_CUENTA = "^[0-9]{10}$";
    private static final String MENSAJE_NUMERO_CUENTA = "El numero de cuenta debe contener 10 digitos";

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @Operation(summary = "Consultar una cuenta por numero de cuenta",
            description = "Un CLIENTE solo puede consultar sus propias cuentas")
    @PreAuthorize(Permisos.EJECUTIVO_O_DUENO_DE_LA_CUENTA)
    @GetMapping(value = "/cuentas/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaDetalleResponse> consultarCuenta(
            @PathVariable @Pattern(regexp = NUMERO_CUENTA, message = MENSAJE_NUMERO_CUENTA) String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.consultarCuenta(numeroCuenta), HttpStatus.OK);
    }

    @Operation(summary = "Consultar el saldo de una cuenta",
            description = "Un CLIENTE solo puede consultar el saldo de sus propias cuentas")
    @PreAuthorize(Permisos.EJECUTIVO_O_DUENO_DE_LA_CUENTA)
    @GetMapping(value = "/cuentas/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoResponse> consultarSaldo(
            @PathVariable @Pattern(regexp = NUMERO_CUENTA, message = MENSAJE_NUMERO_CUENTA) String numeroCuenta) {
        return new ResponseEntity<>(cuentaService.consultarSaldo(numeroCuenta), HttpStatus.OK);
    }

    @Operation(summary = "Consultar cuentas",
            description = "Filtra por cliente (clienteId) y/o por estatus; sin filtros devuelve todas (paginadas). "
                    + "Un CLIENTE debe enviar su propio clienteId")
    @PreAuthorize(Permisos.EJECUTIVO_O_CLIENTE_DEL_FILTRO)
    @GetMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentasResponse> consultarCuentas(@Valid @ParameterObject CuentaFiltro filtro) {
        return new ResponseEntity<>(cuentaService.consultarCuentas(filtro), HttpStatus.OK);
    }

    @Operation(summary = "Crear una cuenta asociada a un cliente (EJECUTIVO)",
            description = "El numero de cuenta, el saldo inicial y el estatus ACTIVA los define el sistema. "
                    + "El cliente debe estar activo")
    @PreAuthorize(Permisos.EJECUTIVO)
    @PostMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaDetalleResponse> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        return new ResponseEntity<>(cuentaService.crearCuenta(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar parcialmente una cuenta (EJECUTIVO)",
            description = "Solo se modifica el estatus. Una cuenta solo puede quedar ACTIVA si su cliente esta activo")
    @PreAuthorize(Permisos.EJECUTIVO)
    @PatchMapping(value = "/cuentas/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaDetalleResponse> actualizarCuenta(
            @PathVariable @Pattern(regexp = NUMERO_CUENTA, message = MENSAJE_NUMERO_CUENTA) String numeroCuenta,
            @Valid @RequestBody ActualizaCuentaRequest request) {
        return new ResponseEntity<>(cuentaService.actualizarCuenta(numeroCuenta, request), HttpStatus.OK);
    }
}
