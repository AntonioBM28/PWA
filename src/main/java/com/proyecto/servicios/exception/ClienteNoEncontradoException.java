package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends OnboardingException {

    public ClienteNoEncontradoException(Integer id) {
        super("No existe un cliente con id " + id, HttpStatus.NOT_FOUND);
    }
}
