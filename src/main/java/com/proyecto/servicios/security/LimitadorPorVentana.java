package com.proyecto.servicios.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Cuenta eventos por clave (una IP, un correo) en ventanas fijas de tiempo: la ventana empieza con
 * el primer evento y dura {@code ventana}; al vencer, el conteo vuelve a cero. Es seguro para uso
 * concurrente (ConcurrentHashMap.compute es atomico por clave) y vive en memoria, por lo que se
 * reinicia al reiniciar la aplicacion.
 */
public class LimitadorPorVentana {

    private record Ventana(long inicio, int conteo) {
    }

    private final int maximo;
    private final long ventanaMs;
    private final LongSupplier reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public LimitadorPorVentana(int maximo, Duration ventana) {
        this(maximo, ventana, System::currentTimeMillis);
    }

    LimitadorPorVentana(int maximo, Duration ventana, LongSupplier reloj) {
        if (maximo < 1) {
            throw new IllegalArgumentException("El maximo de eventos debe ser al menos 1");
        }
        this.maximo = maximo;
        this.ventanaMs = ventana.toMillis();
        this.reloj = reloj;
    }

    /** Registra un evento; devuelve true si sigue dentro del limite. */
    public boolean registrar(String clave) {
        long ahora = reloj.getAsLong();
        Ventana ventana = ventanas.compute(clave, (k, actual) -> actual == null || vencida(actual, ahora)
                ? new Ventana(ahora, 1)
                : new Ventana(actual.inicio(), actual.conteo() + 1));
        return ventana.conteo() <= maximo;
    }

    /** true si la clave ya alcanzo el maximo de eventos en su ventana vigente. */
    public boolean alcanzado(String clave) {
        Ventana ventana = ventanas.get(clave);
        return ventana != null && !vencida(ventana, reloj.getAsLong()) && ventana.conteo() >= maximo;
    }

    /** Segundos que faltan para que venza la ventana de la clave (0 si no hay ventana vigente). */
    public long segundosRestantes(String clave) {
        Ventana ventana = ventanas.get(clave);
        long ahora = reloj.getAsLong();
        if (ventana == null || vencida(ventana, ahora)) {
            return 0;
        }
        return Math.max(1, (ventana.inicio() + ventanaMs - ahora + 999) / 1000);
    }

    public void reiniciar(String clave) {
        ventanas.remove(clave);
    }

    /** Elimina las ventanas vencidas para que la memoria no crezca indefinidamente. */
    public void limpiarVencidas() {
        long ahora = reloj.getAsLong();
        ventanas.values().removeIf(ventana -> vencida(ventana, ahora));
    }

    int claves() {
        return ventanas.size();
    }

    private boolean vencida(Ventana ventana, long ahora) {
        return ahora - ventana.inicio() >= ventanaMs;
    }
}
