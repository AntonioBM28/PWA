package com.proyecto.servicios.security;

import org.springframework.security.core.AuthenticationException;

/** El token es valido, pero su sesion ya se cerro en el servidor (logout u otro dispositivo). */
public class SesionCerradaException extends AuthenticationException {

    public SesionCerradaException(String mensaje) {
        super(mensaje);
    }
}
