package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.DemasiadosIntentosException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.RegistroIntentosLogin;
import com.proyecto.servicios.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String CORREO = "maria.garcia@mail.com";
    private static final String PASSWORD = "Maria#2026";
    private static final int MAX_INTENTOS = 5;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenService tokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(usuarioRepository, passwordEncoder, tokenService,
                new RegistroIntentosLogin(MAX_INTENTOS, 15));
    }

    @Test
    void iniciarSesion_credencialesCorrectas_devuelveToken() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenService.generarToken(usuario)).thenReturn("token-jwt");
        when(tokenService.getVigenciaSegundos()).thenReturn(3600L);

        LoginResponse response = service.iniciarSesion(login(PASSWORD));

        assertEquals("token-jwt", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals(3600L, response.getExpiraEnSegundos());
        assertEquals(CORREO, response.getUsuario());
        assertEquals(2, response.getClienteId());
    }

    @Test
    void iniciarSesion_ejecutivo_devuelveRolSinClienteId() {
        Usuario ejecutivo = new Usuario();
        ejecutivo.setId(50);
        ejecutivo.setCorreo("ejecutivo@banco.com");
        ejecutivo.setPassword(passwordEncoder.encode(PASSWORD));
        ejecutivo.setRol(Rol.EJECUTIVO);
        ejecutivo.setActivo(true);
        when(usuarioRepository.findByCorreo("ejecutivo@banco.com")).thenReturn(Optional.of(ejecutivo));
        when(tokenService.generarToken(ejecutivo)).thenReturn("token-ejecutivo");
        LoginRequest request = login(PASSWORD);
        request.setCorreo("ejecutivo@banco.com");

        LoginResponse response = service.iniciarSesion(request);

        assertEquals(Rol.EJECUTIVO, response.getRol());
        assertNull(response.getClienteId());
    }

    @Test
    void iniciarSesion_correoConMayusculas_seNormaliza() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenService.generarToken(usuario)).thenReturn("token-jwt");

        LoginRequest request = login(PASSWORD);
        request.setCorreo("  Maria.Garcia@Mail.COM ");

        assertEquals("token-jwt", service.iniciarSesion(request).getToken());
    }

    @Test
    void iniciarSesion_usuarioNoExiste_respondeComoCredencialesInvalidas() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        CredencialesInvalidasException ex = assertThrows(UsuarioNoEncontradoException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        // Mismo estatus y mensaje que una contrasena incorrecta: no revela que el correo no existe
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals(new CredencialesInvalidasException().getMessage(), ex.getMessage());
        verify(tokenService, never()).generarToken(any());
    }

    @Test
    void iniciarSesion_contrasenaIncorrecta() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(true)));

        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> service.iniciarSesion(login("Otra#2026")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        verify(tokenService, never()).generarToken(any());
    }

    @Test
    void iniciarSesion_usuarioInactivo_seDeniegaElAcceso() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario(false)));

        UsuarioInactivoException ex = assertThrows(UsuarioInactivoException.class,
                () -> service.iniciarSesion(login(PASSWORD)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        verify(tokenService, never()).generarToken(any());
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
        verify(tokenService, never()).generarToken(any());
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
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
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
        return request;
    }
}
