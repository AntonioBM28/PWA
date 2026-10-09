package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Correo o contrasena incorrectos en el inicio de sesion. El mensaje no indica cual de los dos
 * fallo, para no revelar que correos estan registrados.
 */
public class CredencialesInvalidasException extends OnboardingException {

    static final String MENSAJE = "Correo o contrasena incorrectos";

    public CredencialesInvalidasException() {
        super(MENSAJE, HttpStatus.UNAUTHORIZED);
    }
}
