package com.proyecto.servicios.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorPorVentanaTest {

    private final AtomicLong reloj = new AtomicLong(1_000_000);
    private final LimitadorPorVentana limitador = new LimitadorPorVentana(3, Duration.ofMinutes(1), reloj::get);

    @Test
    void permiteHastaElMaximoYDespuesRechaza() {
        assertTrue(limitador.registrar("ip"));
        assertTrue(limitador.registrar("ip"));
        assertTrue(limitador.registrar("ip"));
        assertTrue(limitador.alcanzado("ip"));

        assertFalse(limitador.registrar("ip"));
    }

    @Test
    void cadaClaveTieneSuPropioConteo() {
        limitador.registrar("ip-1");
        limitador.registrar("ip-1");
        limitador.registrar("ip-1");

        assertTrue(limitador.registrar("ip-2"));
        assertFalse(limitador.alcanzado("ip-2"));
    }

    @Test
    void alVencerLaVentanaElConteoVuelveACero() {
        for (int i = 0; i < 4; i++) {
            limitador.registrar("ip");
        }
        reloj.addAndGet(Duration.ofMinutes(1).toMillis());

        assertFalse(limitador.alcanzado("ip"));
        assertTrue(limitador.registrar("ip"));
    }

    @Test
    void segundosRestantes_cuentaDesdeElPrimerEvento() {
        limitador.registrar("ip");
        reloj.addAndGet(20_000);

        assertEquals(40, limitador.segundosRestantes("ip"));
        assertEquals(0, limitador.segundosRestantes("otra"));
    }

    @Test
    void reiniciar_borraElConteo() {
        limitador.registrar("correo");
        limitador.registrar("correo");
        limitador.registrar("correo");

        limitador.reiniciar("correo");

        assertFalse(limitador.alcanzado("correo"));
    }

    @Test
    void limpiarVencidas_liberaMemoria() {
        limitador.registrar("a");
        limitador.registrar("b");
        reloj.addAndGet(Duration.ofMinutes(2).toMillis());
        limitador.registrar("c");

        limitador.limpiarVencidas();

        assertEquals(1, limitador.claves());
    }

    @Test
    void maximoInvalido() {
        Duration minuto = Duration.ofMinutes(1);
        assertThrows(IllegalArgumentException.class, () -> new LimitadorPorVentana(0, minuto));
    }
}
