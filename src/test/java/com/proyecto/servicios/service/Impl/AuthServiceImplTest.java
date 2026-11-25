package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.DemasiadosIntentosException;
import com.proyecto.servicios.exception.SesionActivaException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.ReenviarCodigoRequest;
import com.proyecto.servicios.model.onboarding.SesionResponse;
import com.proyecto.servicios.model.onboarding.VerificarCodigoRequest;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.RegistroIntentosLogin;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.SesionIniciada;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.service.VerificacionCodigoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String CORREO = "maria.garcia@mail.com";
    private static final String PASSWORD = "Maria#2026";
    private static final String DISPOSITIVO = "dispositivo-1";
    private static final int MAX_INTENTOS = 5;
    private static final Instant AHORA = Instant.parse("2026-11-24T18:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TokenService tokenService;
    @Mock
    private SesionService sesionService;
    @Mock
    private VerificacionCodigoService verificacionService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(usuarioRepository, passwordEncoder, tokenService,
                new RegistroIntentosLogin(MAX_INTENTOS, 15), sesionService, verificacionService,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    // ================================================================ Paso 1: contrasena

    @Test
    void iniciarSesion_credencialesCorrectas_enviaElCodigoSinEntregarToken() {
        Usuario usuario = usuario(true);
        DesafioVerificacionResponse desafio = new DesafioVerificacionResponse();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(verificacionService.crearYEnviar(usuario, Plataforma.WEB, DISPOSITIVO, false)).thenReturn(desafio);
        LoginRequest request = login(PASSWORD);
        request.setPlataforma(Plataforma.WEB);

        assertSame(desafio, service.iniciarSesion(request));
        verify(sesionService).validarDisponible(20, DISPOSITIVO, false);
        verify(tokenService, never()).generarToken(any(), any());
    }

    @Test
    void iniciarSesion_plataformaNoIndicada_esMovil() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        LoginRequest request = login(PASSWORD);
        request.setPlataforma(null);

        service.iniciarSesion(request);

        verify(verificacionService).crearYEnviar(usuario, Plataforma.MOVIL, DISPOSITIVO, false);
    }

    @Test
    void iniciarSesion_correoConMayusculas_seNormaliza() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));
        LoginRequest request = login(PASSWORD);
        request.setCorreo("  Maria.Garcia@Mail.COM ");

        service.iniciarSesion(request);

        verify(usuarioRepository).findByCorreo(CORREO);
    }

    @Test
    void iniciarSesion_sesionAbiertaEnOtroDispositivo_noEnviaCodigo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));
        doThrow(new SesionActivaException(Plataforma.MOVIL, OffsetDateTime.now()))
                .when(sesionService).validarDisponible(20, DISPOSITIVO, false);

        SesionActivaException ex = assertThrows(SesionActivaException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        verify(verificacionService, never()).crearYEnviar(any(), any(), any(), anyBoolean());
    }

    @Test
    void iniciarSesion_aceptandoCerrarLaSesionAnterior_loIndicaAlDesafio() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        LoginRequest request = login(PASSWORD);
        request.setCerrarSesionAnterior(true);

        service.iniciarSesion(request);

        verify(sesionService).validarDisponible(20, DISPOSITIVO, true);
        verify(verificacionService).crearYEnviar(usuario, Plataforma.MOVIL, DISPOSITIVO, true);
    }

    @Test
    void iniciarSesion_usuarioNoExiste_respondeComoCredencialesInvalidas() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        CredencialesInvalidasException ex = assertThrows(UsuarioNoEncontradoException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        // Mismo estatus y mensaje que una contrasena incorrecta: no revela que el correo no existe
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals(new CredencialesInvalidasException().getMessage(), ex.getMessage());
        verify(verificacionService, never()).crearYEnviar(any(), any(), any(), anyBoolean());
    }

    @Test
    void iniciarSesion_contrasenaIncorrecta_noEnviaCodigo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));

        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> service.iniciarSesion(login("Otra#2026")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        verify(verificacionService, never()).crearYEnviar(any(), any(), any(), anyBoolean());
    }

    @Test
    void iniciarSesion_usuarioInactivo_seDeniegaElAcceso() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(false)));

        UsuarioInactivoException ex = assertThrows(UsuarioInactivoException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        verify(verificacionService, never()).crearYEnviar(any(), any(), any(), anyBoolean());
    }

    @Test
    void iniciarSesion_tras5ContrasenasIncorrectas_bloqueaLaCuentaAunConLaCorrecta() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));
        for (int i = 0; i < MAX_INTENTOS; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("Otra#2026")));
        }

        DemasiadosIntentosException ex = assertThrows(DemasiadosIntentosException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
        assertTrue(ex.getSegundosParaReintentar() > 0);
        // El bloqueo se verifica antes de consultar la base de datos
        verify(usuarioRepository, times(MAX_INTENTOS)).findByCorreo(CORREO);
    }

    @Test
    void iniciarSesion_correoInexistente_tambienSeBloquea_paraNoRevelarQueNoExiste() {
        when(usuarioRepository.findByCorreo("nadie@mail.com")).thenReturn(Optional.empty());
        LoginRequest request = login("Otra#2026");
        request.setCorreo("nadie@mail.com");
        for (int i = 0; i < MAX_INTENTOS; i++) {
            assertThrows(UsuarioNoEncontradoException.class, () -> service.iniciarSesion(request));
        }

        assertThrows(DemasiadosIntentosException.class, () -> service.iniciarSesion(request));
    }

    @Test
    void iniciarSesion_exitoso_reiniciaElConteoDeFallos() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));
        for (int i = 0; i < MAX_INTENTOS - 1; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("Otra#2026")));
        }
        service.iniciarSesion(login(PASSWORD));

        // Tras el exito vuelve a tener todos sus intentos
        for (int i = 0; i < MAX_INTENTOS; i++) {
            assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("Otra#2026")));
        }
    }

    @Test
    void iniciarSesion_usuarioInactivoConContrasenaIncorrecta_noRevelaQueEstaInactivo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(false)));

        assertThrows(CredencialesInvalidasException.class, () -> service.iniciarSesion(login("Otra#2026")));
    }

    // ================================================================ Paso 2: codigo

    @Test
    void verificarCodigo_movil_entregaElTokenEnElCuerpo() {
        Usuario usuario = usuario(true);
        Sesion sesion = prepararVerificacion(usuario, Plataforma.MOVIL, false);

        SesionIniciada resultado = service.verificarCodigo(verificar());

        assertEquals("token-jwt", resultado.respuesta().getToken());
        assertEquals("Bearer", resultado.respuesta().getTipoToken());
        assertEquals(3600L, resultado.respuesta().getExpiraEnSegundos());
        assertEquals(CORREO, resultado.respuesta().getUsuario());
        assertEquals(2, resultado.respuesta().getClienteId());
        assertEquals(Plataforma.MOVIL, resultado.plataforma());
        assertEquals(Duration.ofHours(1), resultado.vigencia());
        verify(tokenService).generarToken(usuario, sesion);
    }

    @Test
    void verificarCodigo_web_noIncluyeElTokenEnElCuerpo() {
        prepararVerificacion(usuario(true), Plataforma.WEB, false);

        SesionIniciada resultado = service.verificarCodigo(verificar());

        assertNull(resultado.respuesta().getToken());
        assertNull(resultado.respuesta().getTipoToken());
        // El controller lo pone en la cookie HttpOnly
        assertEquals("token-jwt", resultado.token());
    }

    @Test
    void verificarCodigo_cierraLaSesionAnteriorSiElUsuarioLoAcepto() {
        Usuario usuario = usuario(true);
        prepararVerificacion(usuario, Plataforma.MOVIL, true);

        service.verificarCodigo(verificar());

        verify(sesionService).abrir(usuario, Plataforma.MOVIL, DISPOSITIVO, true);
    }

    @Test
    void verificarCodigo_ejecutivo_sinClienteId() {
        Usuario ejecutivo = usuario(true);
        ejecutivo.setCliente(null);
        ejecutivo.setRol(Rol.EJECUTIVO);
        prepararVerificacion(ejecutivo, Plataforma.MOVIL, false);

        SesionIniciada resultado = service.verificarCodigo(verificar());

        assertEquals(Rol.EJECUTIVO, resultado.respuesta().getRol());
        assertNull(resultado.respuesta().getClienteId());
    }

    @Test
    void verificarCodigo_usuarioDadoDeBajaMientrasTanto_noAbreSesion() {
        CodigoVerificacion desafio = desafio(Plataforma.MOVIL, false);
        when(verificacionService.validar(desafio.getId(), "123456")).thenReturn(desafio);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario(false)));
        VerificarCodigoRequest request = verificar();
        request.setDesafioId(desafio.getId());

        assertThrows(UsuarioInactivoException.class, () -> service.verificarCodigo(request));
        verify(sesionService, never()).abrir(any(), any(), any(), anyBoolean());
    }

    // ================================================================ Reenvio, consulta y cierre

    @Test
    void reenviarCodigo_delegaEnElServicioDeVerificacion() {
        UUID id = UUID.randomUUID();
        DesafioVerificacionResponse desafio = new DesafioVerificacionResponse();
        when(verificacionService.reenviar(id)).thenReturn(desafio);
        ReenviarCodigoRequest request = new ReenviarCodigoRequest();
        request.setDesafioId(id);

        assertSame(desafio, service.reenviarCodigo(request));
    }

    @Test
    void consultarSesion_devuelveDatosDelTokenYRolDeLaBaseDeDatos() {
        JwtAuthenticationToken autenticacion = autenticacion(UUID.randomUUID(), AHORA.plusSeconds(1800));

        SesionResponse response = service.consultarSesion(autenticacion);

        assertEquals(CORREO, response.getUsuario());
        assertEquals(Rol.CLIENTE, response.getRol());
        assertEquals(2, response.getClienteId());
        assertEquals(1800L, response.getExpiraEnSegundos());
    }

    @Test
    void cerrarSesion_cierraLaSesionDelToken() {
        UUID sesionId = UUID.randomUUID();

        service.cerrarSesion(autenticacion(sesionId, AHORA.plusSeconds(60)));

        verify(sesionService).cerrar(sesionId, 20);
    }

    // ================================================================ Utilidades

    private Sesion prepararVerificacion(Usuario usuario, Plataforma plataforma, boolean cerrarAnterior) {
        CodigoVerificacion desafio = desafio(plataforma, cerrarAnterior);
        Sesion sesion = new Sesion();
        sesion.setId(UUID.randomUUID());
        sesion.setFechaCreacion(OffsetDateTime.ofInstant(AHORA, ZoneOffset.UTC));
        sesion.setFechaExpiracion(sesion.getFechaCreacion().plusHours(1));
        when(verificacionService.validar(any(), any())).thenReturn(desafio);
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(sesionService.abrir(usuario, plataforma, DISPOSITIVO, cerrarAnterior)).thenReturn(sesion);
        when(tokenService.generarToken(usuario, sesion)).thenReturn("token-jwt");
        return sesion;
    }

    private static CodigoVerificacion desafio(Plataforma plataforma, boolean cerrarAnterior) {
        CodigoVerificacion desafio = new CodigoVerificacion();
        desafio.setId(UUID.randomUUID());
        desafio.setUsuarioId(20);
        desafio.setPlataforma(plataforma);
        desafio.setDispositivo(DISPOSITIVO);
        desafio.setCerrarSesionAnterior(cerrarAnterior);
        return desafio;
    }

    private static JwtAuthenticationToken autenticacion(UUID sesionId, Instant expiracion) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256")
                .jti(sesionId.toString())
                .subject("20")
                .claim(TokenService.CLAIM_CORREO, CORREO)
                .claim(TokenService.CLAIM_CLIENTE_ID, 2)
                .expiresAt(expiracion)
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    private Usuario usuario(boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setId(2);
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setCliente(cliente);
        usuario.setCorreo(CORREO);
        usuario.setPassword(passwordEncoder.encode(PASSWORD));
        usuario.setActivo(activo);
        return usuario;
    }

    private static LoginRequest login(String password) {
        LoginRequest request = new LoginRequest();
        request.setCorreo(CORREO);
        request.setPassword(password);
        request.setDispositivo(DISPOSITIVO);
        return request;
    }

    private static VerificarCodigoRequest verificar() {
        VerificarCodigoRequest request = new VerificarCodigoRequest();
        request.setDesafioId(UUID.randomUUID());
        request.setCodigo("123456");
        return request;
    }
}
