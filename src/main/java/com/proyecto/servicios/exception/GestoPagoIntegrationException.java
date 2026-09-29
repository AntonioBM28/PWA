package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error base de integracion con GestoPago. Los mensajes nunca incluyen tokens ni credenciales.
 */
@Getter
public class GestoPagoIntegrationException extends RuntimeException {

    private final HttpStatus httpStatus;

    public GestoPagoIntegrationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public GestoPagoIntegrationException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}
