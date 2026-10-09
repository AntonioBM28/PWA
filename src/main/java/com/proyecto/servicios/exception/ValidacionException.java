package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Dato que no cumple una regla de negocio que no se puede expresar con Bean Validation,
 * por ejemplo una colonia que no pertenece al codigo postal.
 */
public class ValidacionException extends OnboardingException {

    public ValidacionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
