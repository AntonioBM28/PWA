package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.MotivoCierreSesion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.SesionActivaException;
import com.proyecto.servicios.repositorys.onboarding.SesionRepository;
import com.proyecto.servicios.service.SesionService.EstadoSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T18:00:00Z");
    private static final OffsetDateTime AHORA_ODT = OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC);

    @Mock
    private SesionRepository sesionRepository;

    private SesionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SesionServiceImpl(sesionRepository, 60, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void validarDisponible_sesionEnOtroDispositivo_avisaDesdeDondeYCuando() {
        Sesion movil = sesion("movil-1", Plataforma.MOVIL);
        when(sesionRepository.buscarAbiertas(20, AHORA_ODT)).thenReturn(List.of(movil));

        SesionActivaException ex = assertThrows(SesionActivaException.class,
                () -> service.validarDisponible(20, "web-1", false));

        assertEquals(Plataforma.MOVIL, ex.getPlataforma());
        assertEquals(movil.getFechaCreacion(), ex.getIniciadaEn());
    }

    @Test
    void validarDisponible_mismoDispositivo_permiteVolverAEntrar() {
        when(sesionRepository.buscarAbiertas(20, AHORA_ODT)).thenReturn(List.of(sesion("movil-1", Plataforma.MOVIL)));

        assertDoesNotThrow(() -> service.validarDisponible(20, "movil-1", false));
    }

    @Test
    void validarDisponible_dispositivoDesconocido_seTrataComoOtro() {
        when(sesionRepository.buscarAbiertas(20, AHORA_ODT)).thenReturn(List.of(sesion(null, Plataforma.MOVIL)));

        assertThrows(SesionActivaException.class, () -> service.validarDisponible(20, null, false));
    }

    @Test
    void validarDisponible_siYaAceptoCerrarla_noConsulta() {
        service.validarDisponible(20, "web-1", true);

        verifyNoInteractions(sesionRepository);
    }

    @Test
    void abrir_sinSesionesPrevias_creaUnaConLaVigenciaDelToken() {
        when(sesionRepository.bloquearAbiertas(20, AHORA_ODT)).thenReturn(List.of());
        when(sesionRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Sesion sesion = service.abrir(usuario(), Plataforma.WEB, "web-1", false);

        assertEquals(20, sesion.getUsuarioId());
        assertEquals(Plataforma.WEB, sesion.getPlataforma());
        assertEquals("web-1", sesion.getDispositivo());
        assertEquals(AHORA_ODT, sesion.getFechaCreacion());
        assertEquals(AHORA_ODT.plusMinutes(60), sesion.getFechaExpiracion());
    }

    @Test
    void abrir_aceptandoCerrarLaAnterior_laMarcaComoReemplazada() {
        Sesion anterior = sesion("movil-1", Plataforma.MOVIL);
        when(sesionRepository.bloquearAbiertas(20, AHORA_ODT)).thenReturn(List.of(anterior));
        when(sesionRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.abrir(usuario(), Plataforma.WEB, "web-1", true);

        assertEquals(MotivoCierreSesion.REEMPLAZADA, anterior.getMotivoCierre());
        assertEquals(AHORA_ODT, anterior.getFechaCierre());
    }

    @Test
    void abrir_otroDispositivoEntroMientrasSeVerificaba_noAbreUnaSegundaSesion() {
        Sesion otra = sesion("movil-1", Plataforma.MOVIL);
        when(sesionRepository.bloquearAbiertas(20, AHORA_ODT)).thenReturn(List.of(otra));

        assertThrows(SesionActivaException.class, () -> service.abrir(usuario(), Plataforma.WEB, "web-1", false));
        assertNull(otra.getFechaCierre());
        verify(sesionRepository, never()).save(any());
    }

    @Test
    void estado_distingueAbiertaReemplazadaEInvalida() {
        Sesion abierta = sesion("a", Plataforma.WEB);
        Sesion reemplazada = sesion("b", Plataforma.WEB);
        reemplazada.cerrar(MotivoCierreSesion.REEMPLAZADA, AHORA_ODT);
        Sesion cerrada = sesion("c", Plataforma.WEB);
        cerrada.cerrar(MotivoCierreSesion.LOGOUT, AHORA_ODT);
        Sesion expirada = sesion("d", Plataforma.WEB);
        expirada.setFechaExpiracion(AHORA_ODT.minusSeconds(1));
        for (Sesion s : List.of(abierta, reemplazada, cerrada, expirada)) {
            when(sesionRepository.findById(s.getId())).thenReturn(Optional.of(s));
        }

        assertEquals(EstadoSesion.ABIERTA, service.estado(abierta.getId(), 20));
        assertEquals(EstadoSesion.REEMPLAZADA, service.estado(reemplazada.getId(), 20));
        assertEquals(EstadoSesion.INVALIDA, service.estado(cerrada.getId(), 20));
        assertEquals(EstadoSesion.INVALIDA, service.estado(expirada.getId(), 20));
    }

    @Test
    void estado_sesionDeOtroUsuarioOInexistente_esInvalida() {
        Sesion ajena = sesion("a", Plataforma.WEB);
        UUID inexistente = UUID.randomUUID();
        when(sesionRepository.findById(ajena.getId())).thenReturn(Optional.of(ajena));
        when(sesionRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertEquals(EstadoSesion.INVALIDA, service.estado(ajena.getId(), 99));
        assertEquals(EstadoSesion.INVALIDA, service.estado(inexistente, 20));
    }

    @Test
    void cerrar_marcaLogoutSoloSiSigueAbierta() {
        Sesion abierta = sesion("a", Plataforma.MOVIL);
        Sesion reemplazada = sesion("b", Plataforma.MOVIL);
        reemplazada.cerrar(MotivoCierreSesion.REEMPLAZADA, AHORA_ODT.minusMinutes(5));
        when(sesionRepository.findById(abierta.getId())).thenReturn(Optional.of(abierta));
        when(sesionRepository.findById(reemplazada.getId())).thenReturn(Optional.of(reemplazada));

        service.cerrar(abierta.getId(), 20);
        service.cerrar(reemplazada.getId(), 20);

        assertEquals(MotivoCierreSesion.LOGOUT, abierta.getMotivoCierre());
        assertEquals(MotivoCierreSesion.REEMPLAZADA, reemplazada.getMotivoCierre());
    }

    private static Sesion sesion(String dispositivo, Plataforma plataforma) {
        Sesion sesion = new Sesion();
        sesion.setId(UUID.randomUUID());
        sesion.setUsuarioId(20);
        sesion.setPlataforma(plataforma);
        sesion.setDispositivo(dispositivo);
        sesion.setFechaCreacion(AHORA_ODT.minusMinutes(10));
        sesion.setFechaExpiracion(AHORA_ODT.plusMinutes(50));
        return sesion;
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(20);
        return usuario;
    }
}
