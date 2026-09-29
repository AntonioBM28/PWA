package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoAuthenticationException extends GestoPagoIntegrationException {

    public GestoPagoAuthenticationException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
