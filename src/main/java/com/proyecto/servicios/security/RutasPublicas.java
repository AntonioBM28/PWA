package com.proyecto.servicios.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Rutas que no requieren sesion. Se declaran en un solo lugar porque las usan la autorizacion
 * (SeguridadConfig) y la lectura del token desde la cookie (CookieSesionFilter).
 */
public final class RutasPublicas {

    public static final String LOGIN = "/auth/login";
    public static final String VERIFICAR_CODIGO = "/auth/verificar-codigo";
    public static final String REENVIAR_CODIGO = "/auth/reenviar-codigo";

    /** POST publicos: inicio de sesion, registro de clientes y consulta de codigos postales. */
    static final String[] POST = {LOGIN, VERIFICAR_CODIGO, REENVIAR_CODIGO, "/clientes", "/codigos-postales/consulta"};

    /** Cualquier metodo: documentacion, catalogo publico y salud del servicio. */
    static final String[] TODAS = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/productos",
            "/actuator/health", "/error"};

    static final RequestMatcher MATCHER = construir();

    private RutasPublicas() {
    }

    private static RequestMatcher construir() {
        java.util.List<RequestMatcher> matchers = new java.util.ArrayList<>();
        for (String ruta : POST) {
            matchers.add(antMatcher(HttpMethod.POST, ruta));
        }
        for (String ruta : TODAS) {
            matchers.add(antMatcher(ruta));
        }
        return new OrRequestMatcher(matchers);
    }
}
