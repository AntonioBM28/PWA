package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** El usuario existe y sus credenciales son correctas, pero esta inactivo (cliente dado de baja). */
public class UsuarioInactivoException extends OnboardingException {

    public UsuarioInactivoException() {
        super("El usuario esta inactivo; no puede iniciar sesion", HttpStatus.FORBIDDEN);
    }
}
