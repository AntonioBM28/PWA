package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error al consultar la API de codigos postales: CP inexistente, servicio no disponible,
 * tiempo de espera agotado o respuesta invalida.
 */
@Getter
public class CodigoPostalException extends RuntimeException {

    private final HttpStatus httpStatus;

    public CodigoPostalException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public CodigoPostalException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}
