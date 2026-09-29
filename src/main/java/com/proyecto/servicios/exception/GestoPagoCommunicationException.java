package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class GestoPagoCommunicationException extends GestoPagoIntegrationException {

    public GestoPagoCommunicationException(String message, Throwable cause) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
