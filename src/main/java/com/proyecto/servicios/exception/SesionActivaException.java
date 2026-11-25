package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.Plataforma;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/**
 * El usuario ya tiene una sesion abierta en otro dispositivo. La app debe preguntarle si quiere
 * cerrarla y repetir el inicio de sesion con cerrarSesionAnterior=true.
 */
@Getter
public class SesionActivaException extends OnboardingException {

    private final Plataforma plataforma;
    private final OffsetDateTime iniciadaEn;

    public SesionActivaException(Plataforma plataforma, OffsetDateTime iniciadaEn) {
        super("Ya tienes una sesion activa en otro dispositivo. Para continuar aqui, cierra esa sesion",
                HttpStatus.CONFLICT);
        this.plataforma = plataforma;
        this.iniciadaEn = iniciadaEn;
    }
}
