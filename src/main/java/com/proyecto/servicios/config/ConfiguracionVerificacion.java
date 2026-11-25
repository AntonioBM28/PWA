package com.proyecto.servicios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Reglas del codigo de verificacion (onboarding.verificacion.* en application.properties).
 *
 * @param vigencia      tiempo que el codigo es valido
 * @param maxIntentos   codigos incorrectos permitidos por desafio antes de invalidarlo
 * @param maxReenvios   veces que se puede pedir un codigo nuevo para el mismo desafio
 * @param esperaReenvio tiempo minimo entre dos envios
 */
@ConfigurationProperties(prefix = "onboarding.verificacion")
public record ConfiguracionVerificacion(Duration vigencia, int maxIntentos, int maxReenvios, Duration esperaReenvio) {
}
