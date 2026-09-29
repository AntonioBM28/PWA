package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Respuesta no exitosa del servicio externo (HTTP no 2xx o cuerpo invalido).
 */
@Getter
public class GestoPagoResponseException extends GestoPagoIntegrationException {

    private final Integer statusExterno;

    public GestoPagoResponseException(String message, Integer statusExterno) {
        super(message, HttpStatus.BAD_GATEWAY);
        this.statusExterno = statusExterno;
    }
}
