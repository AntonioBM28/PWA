package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.AutorizacionTransaccion;
import com.proyecto.servicios.exception.NipException;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionRequest;
import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionResponse;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.DatosCompra;
import com.proyecto.servicios.repositorys.onboarding.AutorizacionTransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacionTransaccionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");

    @Mock
    private AutorizacionTransaccionRepository repository;
    @Mock
    private NipService nipService;
    @Mock
    private TransaccionService transaccionService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AutorizacionTransaccionService service;
    private final JwtAuthenticationToken auth = new JwtAuthenticationToken(
            Jwt.withTokenValue("jwt").header("alg", "HS256").subject("5").claim("clienteId", 7).build(),
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

    @BeforeEach
    void setUp() {
        service = new AutorizacionTransaccionService(repository, nipService, transaccionService, encoder,
                Clock.fixed(AHORA, ZoneOffset.UTC), Duration.ofSeconds(60));
    }

    private static <T extends DatosCompra> T datos(T request) {
        request.setNumeroCuenta("1000000009");
        request.setIdServicio(79);
        request.setIdProducto(200);
        request.setDestino("5512345678");
        request.setMonto(new BigDecimal("100.00"));
        return request;
    }

    @Test
    void autorizar_validaCompraYNipYEmiteTokenDe8DigitosDe60Segundos() {
        AutorizacionTransaccionRequest request = datos(new AutorizacionTransaccionRequest());
        request.setNip("2580");
        when(transaccionService.huella(eq("1000000009"), any())).thenReturn("huella-1");

        AutorizacionTransaccionResponse response = service.autorizar(request, auth);

        verify(transaccionService).validarCompra(request);
        verify(nipService).verificar(5, "2580");
        assertTrue(response.getTokenDinamico().matches("^[0-9]{8}$"));
        assertEquals(60, response.getVigenciaSegundos());
        assertEquals(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusSeconds(60), response.getExpiraEn());
        ArgumentCaptor<AutorizacionTransaccion> guardada = ArgumentCaptor.forClass(AutorizacionTransaccion.class);
        verify(repository).save(guardada.capture());
        assertEquals("huella-1", guardada.getValue().getHuellaSolicitud());
        assertNotEquals(response.getTokenDinamico(), guardada.getValue().getTokenHash());
        assertTrue(encoder.matches(response.getTokenDinamico(), guardada.getValue().getTokenHash()));
    }

    @Test
    void autorizar_compraInvalida_noGastaIntentoDeNip() {
        AutorizacionTransaccionRequest request = datos(new AutorizacionTransaccionRequest());
        request.setNip("2580");
        doThrow(new TransaccionException("monto", HttpStatus.UNPROCESSABLE_ENTITY))
                .when(transaccionService).validarCompra(request);

        assertThrows(TransaccionException.class, () -> service.autorizar(request, auth));
        verify(nipService, never()).verificar(any(), anyString());
        verify(repository, never()).save(any());
    }

    private AutorizacionTransaccion autorizacion(String token, OffsetDateTime expira) {
        AutorizacionTransaccion a = new AutorizacionTransaccion();
        a.setId(UUID.randomUUID());
        a.setUsuarioId(5);
        a.setHuellaSolicitud("huella-1");
        a.setTokenHash(encoder.encode(token));
        a.setFechaExpiracion(expira);
        return a;
    }

    private CrearTransaccionRequest compra(AutorizacionTransaccion a, String token) {
        CrearTransaccionRequest request = datos(new CrearTransaccionRequest());
        request.setAutorizacionId(a.getId());
        request.setTokenDinamico(token);
        return request;
    }

    @Test
    void consumir_tokenCorrecto_unSoloUso() {
        AutorizacionTransaccion a = autorizacion("48213907", OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusSeconds(30));
        when(repository.bloquear(a.getId())).thenReturn(Optional.of(a));
        when(transaccionService.huella(eq("1000000009"), any())).thenReturn("huella-1");

        service.consumir(compra(a, "48213907"), "idem-1234", auth);

        assertNotNull(a.getFechaUso());
        // Reintento de la misma compra (misma Idempotency-Key): permitido
        assertDoesNotThrow(() -> service.consumir(compra(a, "48213907"), "idem-1234", auth));
        // Otra compra con el mismo token: rechazada
        NipException e = assertThrows(NipException.class,
                () -> service.consumir(compra(a, "48213907"), "idem-otra", auth));
        assertEquals(HttpStatus.GONE, e.getHttpStatus());
    }

    @Test
    void consumir_tokenExpirado() {
        AutorizacionTransaccion a = autorizacion("48213907", OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).minusSeconds(1));
        when(repository.bloquear(a.getId())).thenReturn(Optional.of(a));

        NipException e = assertThrows(NipException.class, () -> service.consumir(compra(a, "48213907"), "idem-1234", auth));
        assertTrue(e.getMessage().contains("expiro"));
    }

    @Test
    void consumir_datosDistintosALosAutorizados() {
        AutorizacionTransaccion a = autorizacion("48213907", OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusSeconds(30));
        when(repository.bloquear(a.getId())).thenReturn(Optional.of(a));
        when(transaccionService.huella(eq("1000000009"), any())).thenReturn("otra-huella");

        NipException e = assertThrows(NipException.class, () -> service.consumir(compra(a, "48213907"), "idem-1234", auth));
        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    @Test
    void consumir_tokenIncorrectoCuentaIntentos_yNoSirveDeOtroUsuario() {
        AutorizacionTransaccion a = autorizacion("48213907", OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusSeconds(30));
        when(repository.bloquear(a.getId())).thenReturn(Optional.of(a));
        when(transaccionService.huella(eq("1000000009"), any())).thenReturn("huella-1");

        assertThrows(NipException.class, () -> service.consumir(compra(a, "00000000"), "idem-1234", auth));
        assertEquals(1, a.getIntentosFallidos());

        a.setUsuarioId(99);
        NipException e = assertThrows(NipException.class, () -> service.consumir(compra(a, "48213907"), "idem-1234", auth));
        assertEquals(HttpStatus.GONE, e.getHttpStatus());
    }
}
