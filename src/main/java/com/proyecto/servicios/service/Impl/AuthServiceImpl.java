package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.RegistroIntentosLogin;
import com.proyecto.servicios.security.TokenService;
import com.proyecto.servicios.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Inicio de sesion con correo y contrasena.
 * <p>
 * El orden de las validaciones evita revelar informacion: primero se comprueba la contrasena y
 * solo despues si el usuario esta activo, asi solo quien conoce la contrasena sabe que la cuenta
 * esta inactiva. Si el correo no existe se compara contra un hash de relleno para que la
 * respuesta tarde lo mismo que con un correo registrado.
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_LOGIN = "Inicio de sesion exitoso";
    static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RegistroIntentosLogin registroIntentos;
    private final String hashDeRelleno;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           TokenService tokenService, RegistroIntentosLogin registroIntentos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.registroIntentos = registroIntentos;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-igualar-tiempos");
    }

    @Override
    public LoginResponse iniciarSesion(LoginRequest request) {
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

        LoginResponse response = new LoginResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_LOGIN);
        response.setToken(tokenService.generarToken(usuario));
        response.setTipoToken(TIPO_TOKEN);
        response.setExpiraEnSegundos(tokenService.getVigenciaSegundos());
        response.setUsuario(usuario.getCorreo());
        response.setRol(usuario.getRol());
        // Un EJECUTIVO no es cliente: clienteId queda nulo
        response.setClienteId(usuario.getCliente() == null ? null : usuario.getCliente().getId());
        log.info("Inicio de sesion exitoso: usuario id={}", usuario.getId());
        return response;
    }
}
