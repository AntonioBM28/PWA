package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Los errores de seguridad ocurren en los filtros, antes de llegar a los controllers, por eso no
 * los atrapa GlobalExceptionHandler. Esta clase los responde con el mismo formato GenericResponse.
 */
@Component
public class RespuestaErrorSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAJE_SIN_TOKEN = "Se requiere autenticacion: envie el header Authorization: Bearer <token>";
    static final String MENSAJE_TOKEN_INVALIDO = "El token es invalido o ya expiro; inicie sesion de nuevo";
    static final String MENSAJE_USUARIO_INACTIVO = "El usuario esta inactivo; el token ya no es valido";
    static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta operacion";

    private final ObjectMapper objectMapper;

    public RespuestaErrorSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401: falta el token, es invalido, expiro o el usuario ya no esta activo. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        RespuestaJson.escribirError(response, objectMapper, HttpStatus.UNAUTHORIZED, mensaje(ex));
    }

    /** 403: autenticado, pero sin permiso. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        RespuestaJson.escribirError(response, objectMapper, HttpStatus.FORBIDDEN, MENSAJE_ACCESO_DENEGADO);
    }

    private static String mensaje(AuthenticationException ex) {
        if (ex instanceof DisabledException || ex.getCause() instanceof DisabledException) {
            return MENSAJE_USUARIO_INACTIVO;
        }
        if (ex instanceof InvalidBearerTokenException || ex instanceof BadCredentialsException) {
            return MENSAJE_TOKEN_INVALIDO;
        }
        return MENSAJE_SIN_TOKEN;
    }
}
