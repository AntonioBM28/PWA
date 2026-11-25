package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** El codigo de verificacion es incorrecto, expiro, ya se uso o supero sus intentos. */
public class CodigoVerificacionException extends OnboardingException {

    public CodigoVerificacionException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
