package com.proyecto.servicios.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable (en produccion el del sistema; en pruebas una hora fija) y registro de las
 * propiedades tipadas.
 */
@Configuration
@EnableConfigurationProperties(ConfiguracionVerificacion.class)
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
