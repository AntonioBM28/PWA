package com.proyecto.servicios.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneradorCodigoVerificacionTest {

    private final GeneradorCodigoVerificacion generador = new GeneradorCodigoVerificacion();

    @Test
    void generaCodigosDeSeisDigitosConCerosALaIzquierda() {
        for (int i = 0; i < 1_000; i++) {
            assertTrue(generador.generar().matches("^\\d{6}$"));
        }
    }

    @Test
    void losCodigosNoSeRepitenDeFormaPredecible() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            codigos.add(generador.generar());
        }
        // Con 1,000,000 de valores posibles, 200 codigos casi nunca repiten mas de un par
        assertTrue(codigos.size() > 195);
    }
}
