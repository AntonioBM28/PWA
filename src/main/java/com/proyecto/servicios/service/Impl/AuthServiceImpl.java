package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.CodigoVerificacionException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.ReenviarCodigoRequest;
import com.proyecto.servicios.model.onboarding.SesionResponse;
import com.proyecto.servicios.model.onboarding.VerificarCodigoRequest;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.RegistroIntentosLogin;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.SesionIniciada;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.service.VerificacionCodigoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Inicio de sesion en dos pasos (contrasena y codigo por correo) con una sola sesion por usuario.
 * <p>
 * El orden de las validaciones evita revelar informacion: primero se comprueba la contrasena y
 * solo despues si el usuario esta activo o si tiene otra sesion abierta, asi solo quien conoce la
 * contrasena obtiene esos datos. Si el correo no existe se compara contra un hash de relleno para
 * que la respuesta tarde lo mismo que con un correo registrado.
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_LOGIN = "Inicio de sesion exitoso";
    static final String MENSAJE_SESION = "Sesion activa";
    static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RegistroIntentosLogin registroIntentos;
    private final SesionService sesionService;
    private final VerificacionCodigoService verificacionService;
    private final Clock reloj;
    private final String hashDeRelleno;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           TokenService tokenService, RegistroIntentosLogin registroIntentos,
                           SesionService sesionService, VerificacionCodigoService verificacionService,
                           Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.registroIntentos = registroIntentos;
        this.sesionService = sesionService;
        this.verificacionService = verificacionService;
        this.reloj = reloj;
        // Valor aleatorio en cada arranque: nadie conoce el texto original y no hay credencial fija en el codigo
        this.hashDeRelleno = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    public DesafioVerificacionResponse iniciarSesion(LoginRequest request) {
        String correo = request.getCorreo();
        // Antes de la base de datos y de BCrypt: una cuenta bloqueada no consume recursos
        registroIntentos.verificarNoBloqueado(correo);

        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(request.getPassword(), hashDeRelleno);
            registroIntentos.registrarFallo(correo);
            log.warn("Inicio de sesion rechazado: usuario no encontrado");
            throw new UsuarioNoEncontradoException();
        }
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            registroIntentos.registrarFallo(correo);
            log.warn("Inicio de sesion rechazado: contrasena incorrecta, usuario id={}", usuario.getId());
            throw new CredencialesInvalidasException();
        }
        registroIntentos.registrarExito(correo);
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Inicio de sesion rechazado: usuario inactivo id={}", usuario.getId());
            throw new UsuarioInactivoException();
        }
        // Se avisa antes de enviar el correo, para no mandar un codigo que no podra usarse
        sesionService.validarDisponible(usuario.getId(), request.getDispositivo(), request.isCerrarSesionAnterior());
        return verificacionService.crearYEnviar(usuario, request.getPlataforma(), request.getDispositivo(),
                request.isCerrarSesionAnterior());
    }

    @Override
    @Transactional(noRollbackFor = CodigoVerificacionException.class)
    public SesionIniciada verificarCodigo(VerificarCodigoRequest request) {
        CodigoVerificacion desafio = verificacionService.validar(request.getDesafioId(), request.getCodigo());
        Usuario usuario = usuarioRepository.findById(desafio.getUsuarioId())
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(UsuarioInactivoException::new);

        Sesion sesion = sesionService.abrir(usuario, desafio.getPlataforma(), desafio.getDispositivo(),
                desafio.isCerrarSesionAnterior());
        String token = tokenService.generarToken(usuario, sesion);
        Duration vigencia = Duration.between(sesion.getFechaCreacion(), sesion.getFechaExpiracion());

        LoginResponse response = new LoginResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_LOGIN);
        // En WEB el token viaja en una cookie HttpOnly y no se incluye en el cuerpo
        if (desafio.getPlataforma() == Plataforma.MOVIL) {
            response.setToken(token);
            response.setTipoToken(TIPO_TOKEN);
        }
        response.setExpiraEnSegundos(vigencia.toSeconds());
        response.setUsuario(usuario.getCorreo());
        response.setRol(usuario.getRol());
        // Un EJECUTIVO no es cliente: clienteId queda nulo
        response.setClienteId(usuario.getCliente() == null ? null : usuario.getCliente().getId());
        log.info("Inicio de sesion exitoso: usuario id={}, plataforma={}", usuario.getId(), desafio.getPlataforma());
        return new SesionIniciada(response, desafio.getPlataforma(), token, vigencia);
    }

    @Override
    public DesafioVerificacionResponse reenviarCodigo(ReenviarCodigoRequest request) {
        return verificacionService.reenviar(request.getDesafioId());
    }

    @Override
    public SesionResponse consultarSesion(JwtAuthenticationToken autenticacion) {
        Jwt jwt = autenticacion.getToken();
        SesionResponse response = new SesionResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_SESION);
        response.setUsuario(jwt.getClaimAsString(TokenService.CLAIM_CORREO));
        // El rol se toma de la autenticacion, que lo leyo de la base de datos
        response.setRol(autenticacion.getAuthorities().stream()
                .map(autoridad -> autoridad.getAuthority().replace("ROLE_", ""))
                .map(Rol::valueOf)
                .findFirst()
                .orElseThrow());
        Number clienteId = jwt.getClaim(TokenService.CLAIM_CLIENTE_ID);
        response.setClienteId(clienteId == null ? null : clienteId.intValue());
        Instant expiracion = jwt.getExpiresAt();
        response.setExpiraEnSegundos(expiracion == null ? 0
                : Math.max(0, Duration.between(Instant.now(reloj), expiracion).toSeconds()));
        return response;
    }

    @Override
    public void cerrarSesion(JwtAuthenticationToken autenticacion) {
        Jwt jwt = autenticacion.getToken();
        sesionService.cerrar(UUID.fromString(jwt.getId()), Integer.valueOf(jwt.getSubject()));
        log.info("Sesion cerrada por el usuario: usuario id={}", jwt.getSubject());
    }
}
