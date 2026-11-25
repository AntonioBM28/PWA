package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Los errores de seguridad ocurren en los filtros, antes de llegar a los controllers, por eso no
 * los atrapa GlobalExceptionHandler. Esta clase los responde con el mismo formato GenericResponse.
 */
@Component
public class RespuestaErrorSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAJE_SIN_TOKEN = "Inicia sesion para continuar";
    static final String MENSAJE_TOKEN_INVALIDO = "Tu sesion es invalida o ya expiro; inicia sesion de nuevo";
    static final String MENSAJE_USUARIO_INACTIVO = "El usuario esta inactivo; el token ya no es valido";
    static final String MENSAJE_SESION_CERRADA = "Tu sesion se cerro o expiro; inicia sesion de nuevo";
    static final String MENSAJE_SESION_REEMPLAZADA =
            "Tu sesion se cerro porque iniciaste sesion en otro dispositivo";
    static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta operacion";
    static final String MENSAJE_CSRF = "La solicitud no incluye un token de seguridad valido; recarga la pagina";

    private final ObjectMapper objectMapper;
    private final CookieSesion cookieSesion;

    public RespuestaErrorSeguridad(ObjectMapper objectMapper, CookieSesion cookieSesion) {
        this.objectMapper = objectMapper;
        this.cookieSesion = cookieSesion;
    }

    /** 401: falta el token, es invalido, expiro, su sesion se cerro o el usuario ya no esta activo. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        // La cookie de una sesion que ya no sirve se borra para que el navegador deje de enviarla
        if (CookieSesion.leer(request).isPresent()) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString());
        }
        RespuestaJson.escribirError(response, objectMapper, HttpStatus.UNAUTHORIZED, mensaje(ex));
    }

    /** 403: autenticado pero sin permiso, o una peticion de la web sin token CSRF valido. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        String mensaje = ex instanceof CsrfException ? MENSAJE_CSRF : MENSAJE_ACCESO_DENEGADO;
        RespuestaJson.escribirError(response, objectMapper, HttpStatus.FORBIDDEN, mensaje);
    }

    private static String mensaje(AuthenticationException ex) {
        if (ex instanceof SesionCerradaException || ex.getCause() instanceof SesionCerradaException) {
            return ex instanceof SesionCerradaException ? ex.getMessage() : ex.getCause().getMessage();
        }
        if (ex instanceof DisabledException || ex.getCause() instanceof DisabledException) {
            return MENSAJE_USUARIO_INACTIVO;
        }
        if (ex instanceof InvalidBearerTokenException || ex instanceof BadCredentialsException) {
            return MENSAJE_TOKEN_INVALIDO;
        }
        return MENSAJE_SIN_TOKEN;
    }
}
