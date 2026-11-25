package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.ConfiguracionVerificacion;
import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.CodigoVerificacionException;
import com.proyecto.servicios.exception.CorreoNoEnviadoException;
import com.proyecto.servicios.exception.DemasiadosIntentosException;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.repositorys.onboarding.CodigoVerificacionRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.GeneradorCodigoVerificacion;
import com.proyecto.servicios.service.NotificadorCorreo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionCodigoServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T18:00:00Z");
    private static final OffsetDateTime AHORA_ODT = OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC);
    private static final String CORREO = "maria.garcia@mail.com";
    private static final ConfiguracionVerificacion CONFIG =
            new ConfiguracionVerificacion(Duration.ofMinutes(5), 5, 3, Duration.ofSeconds(60));

    @Mock
    private CodigoVerificacionRepository codigoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private GeneradorCodigoVerificacion generador;
    @Mock
    private NotificadorCorreo notificador;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private VerificacionCodigoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = conReloj(AHORA);
    }

    private VerificacionCodigoServiceImpl conReloj(Instant instante) {
        return new VerificacionCodigoServiceImpl(codigoRepository, usuarioRepository, generador, notificador,
                passwordEncoder, CONFIG, Clock.fixed(instante, ZoneOffset.UTC));
    }

    // ================================================================ crearYEnviar

    @Test
    void crearYEnviar_guardaSoloElHashYEnviaElCodigo() {
        when(generador.generar()).thenReturn("123456");

        DesafioVerificacionResponse response = service.crearYEnviar(usuario(), Plataforma.WEB, "disp", true);

        ArgumentCaptor<CodigoVerificacion> guardado = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(codigoRepository).save(guardado.capture());
        CodigoVerificacion desafio = guardado.getValue();
        assertNotEquals("123456", desafio.getCodigoHash());
        assertTrue(passwordEncoder.matches("123456", desafio.getCodigoHash()));
        assertEquals(Plataforma.WEB, desafio.getPlataforma());
        assertTrue(desafio.isCerrarSesionAnterior());
        assertEquals(AHORA_ODT.plusMinutes(5), desafio.getFechaExpiracion());
        verify(notificador).enviarCodigoVerificacion(CORREO, "123456", Duration.ofMinutes(5));

        assertEquals(desafio.getId(), response.getDesafioId());
        assertEquals("ma***@mail.com", response.getCorreoEnmascarado());
        assertEquals(300L, response.getExpiraEnSegundos());
        assertEquals(60L, response.getReenviarEnSegundos());
    }

    @Test
    void crearYEnviar_siElCorreoFalla_propagaElError() {
        when(generador.generar()).thenReturn("123456");
        doThrow(new CorreoNoEnviadoException()).when(notificador).enviarCodigoVerificacion(anyString(), anyString(), any());

        CorreoNoEnviadoException ex = assertThrows(CorreoNoEnviadoException.class,
                () -> service.crearYEnviar(usuario(), Plataforma.MOVIL, null, false));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
    }

    // ================================================================ validar

    @Test
    void validar_codigoCorrecto_loMarcaComoUsado() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacion validado = service.validar(desafio.getId(), "123456");

        assertEquals(AHORA_ODT, validado.getFechaUso());
    }

    @Test
    void validar_codigoIncorrecto_cuentaElIntentoEIndicaLosRestantes() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> service.validar(desafio.getId(), "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("El codigo es incorrecto. Te quedan 4 intentos", ex.getMessage());
        assertEquals(1, desafio.getIntentosFallidos());
    }

    @Test
    void validar_ultimoIntentoFallido_bloqueaElDesafio() {
        CodigoVerificacion desafio = desafio("123456");
        desafio.setIntentosFallidos((short) 4);
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> service.validar(desafio.getId(), "000000"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        assertEquals(VerificacionCodigoServiceImpl.MENSAJE_SIN_INTENTOS, ex.getMessage());
    }

    @Test
    void validar_sinIntentos_rechazaAunElCodigoCorrecto() {
        CodigoVerificacion desafio = desafio("123456");
        desafio.setIntentosFallidos((short) 5);
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> service.validar(desafio.getId(), "123456"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
    }

    @Test
    void validar_codigoYaUsado_noSePuedeReutilizar() {
        CodigoVerificacion desafio = desafio("123456");
        desafio.setFechaUso(AHORA_ODT.minusMinutes(1));
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> service.validar(desafio.getId(), "123456"));

        assertEquals("Este codigo ya se utilizo; inicia sesion de nuevo", ex.getMessage());
    }

    @Test
    void validar_codigoExpirado_esRechazado() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> conReloj(AHORA.plus(Duration.ofMinutes(6))).validar(desafio.getId(), "123456"));

        assertEquals("El codigo expiro; solicita uno nuevo", ex.getMessage());
    }

    @Test
    void validar_desafioInexistente_esRechazado() {
        UUID id = UUID.randomUUID();
        when(codigoRepository.bloquear(id)).thenReturn(Optional.empty());

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> service.validar(id, "123456"));

        assertEquals(VerificacionCodigoServiceImpl.MENSAJE_NO_VALIDO, ex.getMessage());
    }

    // ================================================================ reenviar

    @Test
    void reenviar_antesDeLaEspera_indicaCuantoFalta() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> conReloj(AHORA.plusSeconds(20)).reenviar(desafio.getId()));

        assertEquals(40L, ex.getSegundosParaReintentar());
        verify(notificador, never()).enviarCodigoVerificacion(anyString(), anyString(), any());
    }

    @Test
    void reenviar_generaUnCodigoNuevoYElAnteriorDejaDeServir() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario()));
        when(generador.generar()).thenReturn("654321");
        Instant despues = AHORA.plusSeconds(90);

        DesafioVerificacionResponse response = conReloj(despues).reenviar(desafio.getId());

        assertTrue(passwordEncoder.matches("654321", desafio.getCodigoHash()));
        assertFalse(passwordEncoder.matches("123456", desafio.getCodigoHash()));
        assertEquals(1, desafio.getReenvios());
        assertEquals(OffsetDateTime.ofInstant(despues, ZoneOffset.UTC).plusMinutes(5), desafio.getFechaExpiracion());
        verify(notificador).enviarCodigoVerificacion(CORREO, "654321", Duration.ofMinutes(5));
        assertNotNull(response.getDesafioId());
    }

    @Test
    void reenviar_superaElLimite_pideIniciarSesionDeNuevo() {
        CodigoVerificacion desafio = desafio("123456");
        desafio.setReenvios((short) 3);
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> conReloj(AHORA.plusSeconds(90)).reenviar(desafio.getId()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
    }

    @Test
    void reenviar_codigoExpirado_permiteSolicitarUnoNuevo() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario()));
        when(generador.generar()).thenReturn("777777");

        conReloj(AHORA.plus(Duration.ofMinutes(10))).reenviar(desafio.getId());

        assertTrue(passwordEncoder.matches("777777", desafio.getCodigoHash()));
    }

    @Test
    void reenviar_desafioDeMasDe30Minutos_yaNoSeRenueva() {
        CodigoVerificacion desafio = desafio("123456");
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        CodigoVerificacionException ex = assertThrows(CodigoVerificacionException.class,
                () -> conReloj(AHORA.plus(Duration.ofMinutes(31))).reenviar(desafio.getId()));

        assertEquals(HttpStatus.GONE, ex.getHttpStatus());
    }

    @Test
    void reenviar_desafioYaUsado_esRechazado() {
        CodigoVerificacion desafio = desafio("123456");
        desafio.setFechaUso(AHORA_ODT);
        when(codigoRepository.bloquear(desafio.getId())).thenReturn(Optional.of(desafio));

        assertThrows(CodigoVerificacionException.class, () -> service.reenviar(desafio.getId()));
    }

    @Test
    void enmascarar_ocultaElCorreo() {
        assertEquals("ma***@mail.com", VerificacionCodigoServiceImpl.enmascarar("maria@mail.com"));
        assertEquals("a***@mail.com", VerificacionCodigoServiceImpl.enmascarar("a@mail.com"));
        assertEquals("***", VerificacionCodigoServiceImpl.enmascarar("sin-arroba"));
    }

    // ================================================================ Utilidades

    private CodigoVerificacion desafio(String codigo) {
        CodigoVerificacion desafio = new CodigoVerificacion();
        desafio.setId(UUID.randomUUID());
        desafio.setUsuarioId(20);
        desafio.setPlataforma(Plataforma.MOVIL);
        desafio.setCodigoHash(passwordEncoder.encode(codigo));
        desafio.setFechaCreacion(AHORA_ODT);
        desafio.setFechaEnvio(AHORA_ODT);
        desafio.setFechaExpiracion(AHORA_ODT.plusMinutes(5));
        return desafio;
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setCorreo(CORREO);
        return usuario;
    }
}
