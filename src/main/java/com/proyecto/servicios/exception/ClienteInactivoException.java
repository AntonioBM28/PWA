package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** Regla de negocio: solo los clientes activos pueden tener cuentas activas. */
public class ClienteInactivoException extends OnboardingException {

    public ClienteInactivoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
