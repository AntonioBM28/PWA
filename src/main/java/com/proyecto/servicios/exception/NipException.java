package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** NIP no configurado, invalido, incorrecto o bloqueado; o token dinamico invalido. */
public class NipException extends OnboardingException {

    public NipException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
