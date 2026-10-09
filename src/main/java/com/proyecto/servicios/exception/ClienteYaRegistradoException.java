package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Ya existe un cliente con alguno de los datos unicos. Los mensajes no repiten el dato recibido.
 */
public class ClienteYaRegistradoException extends OnboardingException {

    public ClienteYaRegistradoException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
