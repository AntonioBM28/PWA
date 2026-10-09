package com.proyecto.servicios.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** Datos de codigos postales; casi no cambian, por eso se guardan en memoria. */
    public static final String CODIGOS_POSTALES = "codigosPostales";
}
