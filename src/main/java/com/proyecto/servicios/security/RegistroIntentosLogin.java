package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bloqueo temporal por cuenta: despues de N contrasenas incorrectas para un mismo correo, ese correo
 * no puede iniciar sesion durante unos minutos, aunque el atacante cambie de IP.
 * <p>
 * Se cuenta por correo exista o no, para que el bloqueo no revele que correos estan registrados.
 * La verificacion ocurre antes de consultar la base de datos y de BCrypt, asi un ataque contra una
 * cuenta bloqueada tampoco consume CPU.
 */
@Component
public class RegistroIntentosLogin {

    private final LimitadorPorVentana fallos;

    @Autowired
    public RegistroIntentosLogin(@Value("${onboarding.login.max-intentos-fallidos}") int maxIntentosFallidos,
                                 @Value("${onboarding.login.bloqueo-minutos}") long bloqueoMinutos) {
        this(new LimitadorPorVentana(maxIntentosFallidos, Duration.ofMinutes(bloqueoMinutos)));
    }

    RegistroIntentosLogin(LimitadorPorVentana fallos) {
        this.fallos = fallos;
    }

    public void verificarNoBloqueado(String correo) {
        if (fallos.alcanzado(correo)) {
            long segundos = fallos.segundosRestantes(correo);
            throw new DemasiadosIntentosException("Demasiados intentos fallidos para esta cuenta; intente de nuevo en "
                    + Math.max(1, (segundos + 59) / 60) + " minuto(s)", segundos);
        }
    }

    public void registrarFallo(String correo) {
        fallos.registrar(correo);
    }

    public void registrarExito(String correo) {
        fallos.reiniciar(correo);
    }

    @Scheduled(fixedRate = 300_000)
    public void limpiar() {
        fallos.limpiarVencidas();
    }
}
