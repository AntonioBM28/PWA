package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.CuerpoDemasiadoGrandeException;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteTamanioCuerpoFilterTest {

    private static final int MAXIMO_KB = 1;

    private final LimiteTamanioCuerpoFilter filtro = new LimiteTamanioCuerpoFilter(MAXIMO_KB, new ObjectMapper());

    @Test
    void contentLengthMayorAlMaximo_respondePayloadTooLargeSinLeerElCuerpo() throws Exception {
        MockHttpServletRequest request = peticion(new byte[2048]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(request, response, cadena);

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("excede el maximo de 1 KB"));
        assertNull(cadena.getRequest(), "la peticion no debe llegar al controller");
    }

    @Test
    void cuerpoDentroDelLimite_continuaYSePuedeLeer() throws Exception {
        MockHttpServletRequest request = peticion("{\"nombre\":\"Jose\"}".getBytes());
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        ServletRequest recibida = cadena.getRequest();
        assertNotNull(recibida);
        assertEquals("{\"nombre\":\"Jose\"}", new String(recibida.getInputStream().readAllBytes()));
    }

    @Test
    void sinContentLength_seCortaAlLeerMasDelMaximo() throws Exception {
        MockFilterChain cadena = new MockFilterChain();
        // Simula un envio por partes: el servidor no conoce el tamano de antemano
        MockHttpServletRequest sinLongitud = new MockHttpServletRequest("POST", "/clientes") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        sinLongitud.setContent(new byte[4096]);

        filtro.doFilter(sinLongitud, new MockHttpServletResponse(), cadena);

        InputStream cuerpo = cadena.getRequest().getInputStream();
        assertThrows(CuerpoDemasiadoGrandeException.class, cuerpo::readAllBytes);
    }

    private static MockHttpServletRequest peticion(byte[] cuerpo) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/clientes");
        request.setContentType("application/json");
        request.setContent(cuerpo);
        return request;
    }
}
