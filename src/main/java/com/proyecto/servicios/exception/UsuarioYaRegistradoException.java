package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** Regla de negocio: cada cliente puede tener unicamente un usuario de acceso. */
public class UsuarioYaRegistradoException extends OnboardingException {

    public UsuarioYaRegistradoException(Integer clienteId) {
        super("El cliente " + clienteId + " ya tiene un usuario de acceso", HttpStatus.CONFLICT);
    }
}
