package com.proyecto.servicios.service;

import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.model.onboarding.LoginResponse;

import java.time.Duration;

/**
 * Resultado interno de verificar el codigo. El controller decide como entregar el token segun la
 * plataforma: en el cuerpo (MOVIL) o en una cookie HttpOnly (WEB).
 */
public record SesionIniciada(LoginResponse respuesta, Plataforma plataforma, String token, Duration vigencia) {
}
