package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CodigoPostalException;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CodigoPostalFeignConfigTest {

    private final ErrorDecoder errorDecoder = new CodigoPostalFeignConfig().codigoPostalErrorDecoder();

    @Test
    void decode_noEncontrado() {
        assertEquals(HttpStatus.NOT_FOUND, decode(404).getHttpStatus());
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 503})
    void decode_servicioNoDisponible(int status) {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, decode(status).getHttpStatus());
    }

    @Test
    void decode_otroError() {
        assertEquals(HttpStatus.BAD_GATEWAY, decode(400).getHttpStatus());
    }

    private CodigoPostalException decode(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/cp/37806",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).request(request).headers(Collections.emptyMap()).build();
        return assertInstanceOf(CodigoPostalException.class, errorDecoder.decode("consultar", response));
    }
}
