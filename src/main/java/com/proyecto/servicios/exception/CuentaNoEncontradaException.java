package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends OnboardingException {

    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No existe la cuenta " + numeroCuenta, HttpStatus.NOT_FOUND);
    }
}
