package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/** No se pudo enviar el correo con el codigo de verificacion. */
public class CorreoNoEnviadoException extends OnboardingException {

    public CorreoNoEnviadoException() {
        super("No fue posible enviar el codigo de verificacion; intenta de nuevo en unos minutos",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
