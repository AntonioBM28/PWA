package com.proyecto.servicios.security;

import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControlAccesoTest {

    private final CuentaRepository cuentaRepository = mock(CuentaRepository.class);
    private final ControlAcceso acceso = new ControlAcceso(cuentaRepository);

    @BeforeEach
    void limpiarAntes() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiarDespues() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cliente_accedeSoloASusPropiosDatos() {
        autenticar("CLIENTE", 1);

        assertTrue(acceso.esCliente(1));
        assertFalse(acceso.esCliente(2));
        assertFalse(acceso.esCliente(null));
    }

    @Test
    void cliente_accedeSoloASusPropiasCuentas() {
        autenticar("CLIENTE", 1);
        when(cuentaRepository.existsByNumeroCuentaAndClienteId("1000000000", 1)).thenReturn(true);
        when(cuentaRepository.existsByNumeroCuentaAndClienteId("1000000001", 1)).thenReturn(false);

        assertTrue(acceso.esCuentaPropia("1000000000"));
        assertFalse(acceso.esCuentaPropia("1000000001"));
    }

    @Test
    void ejecutivo_noTieneClienteId_suAccesoVieneDelRol() {
        autenticar("EJECUTIVO", null);

        assertFalse(acceso.esCliente(1));
        assertFalse(acceso.esCuentaPropia("1000000000"));
        verify(cuentaRepository, never()).existsByNumeroCuentaAndClienteId(anyString(), anyInt());
    }

    @Test
    void sinAutenticacion_noTieneAcceso() {
        assertFalse(acceso.esCliente(1));
        assertFalse(acceso.esCuentaPropia("1000000000"));
    }

    private static void autenticar(String rol, Integer clienteId) {
        Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("10");
        if (clienteId != null) {
            jwt.claim(TokenService.CLAIM_CLIENTE_ID, clienteId);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
