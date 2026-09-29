package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.GestoPagoResponseException;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

/**
 * Configuracion exclusiva del cliente Feign de productos. No se anota con @Configuration
 * para que el Decoder XML y el ErrorDecoder no se apliquen globalmente a los demas clientes.
 * Los timeouts se definen en application.properties (spring.cloud.openfeign.client.config.gestoPagoProductos).
 */
public class GestoPagoProductosFeignConfig {

    @Bean
    public Decoder gestoPagoProductosDecoder() {
        return new GestoPagoXmlDecoder();
    }

    @Bean
    public ErrorDecoder gestoPagoProductosErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
                return new GestoPagoAuthenticationException("GestoPago rechazo las credenciales de acceso");
            }
            return new GestoPagoResponseException("GestoPago respondio con estatus no exitoso", status);
        };
    }
}
