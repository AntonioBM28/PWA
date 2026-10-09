package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitePeticionesFilterTest {

    private final LimitePeticionesFilter filtro = new LimitePeticionesFilter(
            new LimitadorPorVentana(2, Duration.ofMinutes(1)),
            new LimitadorPorVentana(1, Duration.ofMinutes(1)),
            new ObjectMapper());

    @Test
    void login_alSuperarElLimiteRespondeTooManyRequests() throws Exception {
        assertEquals(200, ejecutar("POST", "/auth/login", "10.0.0.1").getStatus());
        assertEquals(200, ejecutar("POST", "/auth/login", "10.0.0.1").getStatus());

        MockHttpServletResponse bloqueada = ejecutar("POST", "/auth/login", "10.0.0.1");

        assertEquals(429, bloqueada.getStatus());
        assertNotNull(bloqueada.getHeader("Retry-After"));
        assertTrue(bloqueada.getContentAsString().contains("Demasiadas solicitudes"));
    }

    @Test
    void elLimiteEsPorIp() throws Exception {
        ejecutar("POST", "/auth/login", "10.0.0.1");
        ejecutar("POST", "/auth/login", "10.0.0.1");

        assertEquals(200, ejecutar("POST", "/auth/login", "10.0.0.2").getStatus());
    }

    @Test
    void registro_tieneSuPropioLimite() throws Exception {
        assertEquals(200, ejecutar("POST", "/clientes", "10.0.0.1").getStatus());
        assertEquals(429, ejecutar("POST", "/clientes", "10.0.0.1").getStatus());
    }

    @Test
    void otrasRutasYMetodos_noSeLimitan() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, ejecutar("GET", "/clientes", "10.0.0.1").getStatus());
            assertEquals(200, ejecutar("PATCH", "/clientes/1", "10.0.0.1").getStatus());
            assertEquals(200, ejecutar("GET", "/codigos-postales/44100", "10.0.0.1").getStatus());
        }
    }

    private MockHttpServletResponse ejecutar(String metodo, String ruta, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setServletPath(ruta);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(request, response, cadena);
        if (response.getStatus() == 200) {
            assertNotNull(cadena.getRequest(), "la peticion permitida debe continuar");
        } else {
            assertNull(cadena.getRequest(), "la peticion rechazada no debe continuar");
        }
        return response;
    }
}
