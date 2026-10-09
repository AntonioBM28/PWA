package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.AgregaUsuarioRequest;
import com.proyecto.servicios.model.onboarding.UsuarioDetalleResponse;
import com.proyecto.servicios.model.onboarding.UsuarioFiltro;
import com.proyecto.servicios.model.onboarding.UsuariosResponse;
import com.proyecto.servicios.security.Permisos;
import com.proyecto.servicios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Administracion de usuarios: exclusiva del EJECUTIVO. */
@RestController
@PreAuthorize(Permisos.EJECUTIVO)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Consultar usuarios",
            description = "Filtra por correo (o inicio del correo), activo y/o clienteId; sin filtros devuelve todos")
    @GetMapping(value = "/usuarios/filtro", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuariosResponse> consultarUsuarios(@Valid @ParameterObject UsuarioFiltro filtro) {
        return new ResponseEntity<>(usuarioService.consultarUsuarios(filtro), HttpStatus.OK);
    }

    @Operation(summary = "Agregar el usuario de acceso de un cliente",
            description = "Para un cliente activo que no tiene usuario. El correo del cliente es el nombre de usuario; "
                    + "cada cliente puede tener un solo usuario")
    @PutMapping(value = "/usuarios/agregar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioDetalleResponse> agregarUsuario(@Valid @RequestBody AgregaUsuarioRequest request) {
        return new ResponseEntity<>(usuarioService.agregarUsuario(request), HttpStatus.CREATED);
    }
}
