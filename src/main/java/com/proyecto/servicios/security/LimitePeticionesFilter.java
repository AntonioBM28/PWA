package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Limite de peticiones por IP en los endpoints publicos costosos: el login (BCrypt) y el registro de
 * clientes (BCrypt, base de datos y API de codigos postales). Al superarlo responde 429 con el header
 * Retry-After. Frena la fuerza bruta de contrasenas y que se use la API para saturar el servidor.
 * <p>
 * Se usa la IP de la conexion (getRemoteAddr). No se confia en X-Forwarded-For porque cualquier
 * cliente puede falsificarlo; si la app se publica detras de un proxy, hay que configurar el proxy
 * para que lo envie y Spring para que lo lea (server.forward-headers-strategy).
 */
@Component
@Slf4j
public class LimitePeticionesFilter extends OncePerRequestFilter {

    static final String RUTA_LOGIN = "/auth/login";
    static final String RUTA_REGISTRO = "/clientes";
    private static final Duration MINUTO = Duration.ofMinutes(1);

    private final LimitadorPorVentana login;
    private final LimitadorPorVentana registro;
    private final ObjectMapper objectMapper;

    @Autowired
    public LimitePeticionesFilter(@Value("${onboarding.limite.login-por-minuto}") int loginPorMinuto,
                                  @Value("${onboarding.limite.registro-por-minuto}") int registroPorMinuto,
                                  ObjectMapper objectMapper) {
        this(new LimitadorPorVentana(loginPorMinuto, MINUTO), new LimitadorPorVentana(registroPorMinuto, MINUTO),
                objectMapper);
    }

    LimitePeticionesFilter(LimitadorPorVentana login, LimitadorPorVentana registro, ObjectMapper objectMapper) {
        this.login = login;
        this.registro = registro;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitador(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorPorVentana limitador = limitador(request);
        String ip = request.getRemoteAddr();
        if (limitador.registrar(ip)) {
            chain.doFilter(request, response);
            return;
        }
        long segundos = limitador.segundosRestantes(ip);
        log.warn("Limite de peticiones superado en {}", request.getServletPath());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        RespuestaJson.escribirError(response, objectMapper, HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas solicitudes desde su direccion; intente de nuevo en " + segundos + " segundos");
    }

    private LimitadorPorVentana limitador(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        return switch (request.getServletPath()) {
            case RUTA_LOGIN -> login;
            case RUTA_REGISTRO -> registro;
            default -> null;
        };
    }

    @Scheduled(fixedRate = 300_000)
    public void limpiar() {
        login.limpiarVencidas();
        registro.limpiarVencidas();
    }
}
