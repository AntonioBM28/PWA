package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error de negocio del onboarding de clientes. Cada subclase define su estatus HTTP y
 * GlobalExceptionHandler la convierte en GenericResponse.
 */
@Getter
public class OnboardingException extends RuntimeException {

    private final HttpStatus httpStatus;

    public OnboardingException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }
}
