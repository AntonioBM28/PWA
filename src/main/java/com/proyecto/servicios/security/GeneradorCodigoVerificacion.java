package com.proyecto.servicios.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Codigos numericos de un solo uso, generados con un generador criptograficamente seguro. */
@Component
public class GeneradorCodigoVerificacion {

    static final int DIGITOS = 6;
    private static final int LIMITE = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    /** Codigo de 6 digitos con ceros a la izquierda (000000 - 999999). */
    public String generar() {
        return String.format("%0" + DIGITOS + "d", random.nextInt(LIMITE));
    }
}
