package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.GestoPagoResponseException;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GestoPagoProductosFeignConfigTest {

    private final ErrorDecoder errorDecoder = new GestoPagoProductosFeignConfig().gestoPagoProductosErrorDecoder();

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void decode_errorDeAutenticacion(int status) {
        Exception ex = errorDecoder.decode("getProductList", response(status));

        assertInstanceOf(GestoPagoAuthenticationException.class, ex);
    }

    @Test
    void decode_errorDelServidor() {
        Exception ex = errorDecoder.decode("getProductList", response(500));

        GestoPagoResponseException responseEx = assertInstanceOf(GestoPagoResponseException.class, ex);
        assertEquals(500, responseEx.getStatusExterno());
    }

    private static Response response(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return Response.builder().status(status).request(request).headers(Collections.emptyMap()).build();
    }
}
