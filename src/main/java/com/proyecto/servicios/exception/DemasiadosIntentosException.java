package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Se superaron los intentos permitidos (proteccion contra fuerza bruta). */
@Getter
public class DemasiadosIntentosException extends OnboardingException {

    private final long segundosParaReintentar;

    public DemasiadosIntentosException(String message, long segundosParaReintentar) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
        this.segundosParaReintentar = segundosParaReintentar;
    }
}
