package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.NipTransaccion;
import com.proyecto.servicios.exception.NipException;
import com.proyecto.servicios.repositorys.onboarding.NipTransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NipServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-27T18:00:00Z");

    @Mock
    private NipTransaccionRepository repository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private NipService service;

    @BeforeEach
    void setUp() {
        service = new NipService(repository, encoder, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private NipTransaccion registro(String nip) {
        NipTransaccion n = new NipTransaccion();
        n.setUsuarioId(5);
        n.setNipHash(encoder.encode(nip));
        return n;
    }

    @Test
    void crear_guardaSoloElHash() {
        when(repository.existsById(5)).thenReturn(false);

        service.crear(5, "2580", "2580");

        ArgumentCaptor<NipTransaccion> guardado = ArgumentCaptor.forClass(NipTransaccion.class);
        verify(repository).save(guardado.capture());
        assertNotEquals("2580", guardado.getValue().getNipHash());
        assertTrue(encoder.matches("2580", guardado.getValue().getNipHash()));
    }

    @Test
    void crear_siYaExiste_conflicto() {
        when(repository.existsById(5)).thenReturn(true);

        NipException e = assertThrows(NipException.class, () -> service.crear(5, "2580", "2580"));
        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1111", "1234", "4321", "0123", "6789", "123", "12a4"})
    void nipsDebilesOInvalidos(String nip) {
        assertThrows(NipException.class, () -> NipService.validarNuevo(nip, nip));
    }

    @Test
    void confirmacionDistinta() {
        NipException e = assertThrows(NipException.class, () -> NipService.validarNuevo("2580", "2581"));
        assertTrue(e.getMessage().contains("confirmacion"));
        assertDoesNotThrow(() -> NipService.validarNuevo("2580", "2580"));
    }

    @Test
    void verificar_correctoReiniciaIntentos() {
        NipTransaccion n = registro("2580");
        n.setIntentosFallidos((short) 2);
        when(repository.bloquear(5)).thenReturn(Optional.of(n));

        service.verificar(5, "2580");

        assertEquals(0, n.getIntentosFallidos());
    }

    @Test
    void verificar_tresFallosBloquea15Minutos() {
        NipTransaccion n = registro("2580");
        when(repository.bloquear(5)).thenReturn(Optional.of(n));

        NipException primero = assertThrows(NipException.class, () -> service.verificar(5, "0000"));
        assertEquals(HttpStatus.BAD_REQUEST, primero.getHttpStatus());
        assertTrue(primero.getMessage().contains("2 intentos"), primero.getMessage());
        assertThrows(NipException.class, () -> service.verificar(5, "0000"));
        NipException tercero = assertThrows(NipException.class, () -> service.verificar(5, "0000"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, tercero.getHttpStatus());
        assertEquals(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC).plusMinutes(15), n.getBloqueadoHasta());
        // Bloqueado: ni el NIP correcto pasa
        assertThrows(NipException.class, () -> service.verificar(5, "2580"));
        when(repository.findById(5)).thenReturn(Optional.of(n));
        assertEquals(n.getBloqueadoHasta(), service.estado(5).bloqueadoHasta());
    }

    @Test
    void verificar_sinNipConfigurado() {
        when(repository.bloquear(5)).thenReturn(Optional.empty());

        NipException e = assertThrows(NipException.class, () -> service.verificar(5, "2580"));
        assertEquals(HttpStatus.CONFLICT, e.getHttpStatus());
    }

    @Test
    void cambiar_requiereElActualYUnoDistinto() {
        NipTransaccion n = registro("2580");
        when(repository.bloquear(5)).thenReturn(Optional.of(n));

        assertThrows(NipException.class, () -> service.cambiar(5, "2580", "2580", "2580"));
        service.cambiar(5, "2580", "7391", "7391");

        assertTrue(encoder.matches("7391", n.getNipHash()));
    }

    @Test
    void estado() {
        when(repository.findById(5)).thenReturn(Optional.empty());
        assertFalse(service.estado(5).configurado());

        NipTransaccion n = registro("2580");
        when(repository.findById(5)).thenReturn(Optional.of(n));
        assertTrue(service.estado(5).configurado());
        assertNull(service.estado(5).bloqueadoHasta());
    }
}
