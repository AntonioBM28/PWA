package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CodigoPostalException;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

/**
 * Configuracion exclusiva del cliente Feign de codigos postales. No se anota con @Configuration
 * para que el ErrorDecoder no se aplique a los demas clientes.
 * Los timeouts se definen en application.properties (spring.cloud.openfeign.client.config.codigoPostal).
 */
public class CodigoPostalFeignConfig {

    static final String MENSAJE_NO_ENCONTRADO = "El codigo postal no existe";
    static final String MENSAJE_NO_DISPONIBLE = "El servicio de codigos postales no esta disponible, intente mas tarde";
    static final String MENSAJE_RESPUESTA_INVALIDA = "El servicio de codigos postales respondio con un error";

    @Bean
    public ErrorDecoder codigoPostalErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            if (status == HttpStatus.NOT_FOUND.value()) {
                return new CodigoPostalException(MENSAJE_NO_ENCONTRADO, HttpStatus.NOT_FOUND);
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS.value() || status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return new CodigoPostalException(MENSAJE_NO_DISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
            }
            return new CodigoPostalException(MENSAJE_RESPUESTA_INVALIDA, HttpStatus.BAD_GATEWAY);
        };
    }
}
