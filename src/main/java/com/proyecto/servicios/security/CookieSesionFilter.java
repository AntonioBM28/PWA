package com.proyecto.servicios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

/**
 * App web: copia el JWT de la cookie de sesion al header Authorization para que la autenticacion
 * estandar de Spring lo valide igual que el de la app movil.
 * <p>
 * Se registra DESPUES del filtro CSRF a proposito. Spring desactiva el CSRF en las peticiones que
 * traen un token Bearer (porque un sitio externo no puede ponerlo); si el token de la cookie se
 * leyera antes, las peticiones de la web quedarian sin proteccion CSRF aunque el navegador envie
 * la cookie solo.
 * <p>
 * En rutas publicas no se copia: una cookie de una sesion ya cerrada no debe impedir volver a
 * iniciar sesion.
 */
public class CookieSesionFilter extends OncePerRequestFilter {

    private final RequestMatcher rutasPublicas;

    public CookieSesionFilter(RequestMatcher rutasPublicas) {
        this.rutasPublicas = rutasPublicas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<String> token = request.getHeader(HttpHeaders.AUTHORIZATION) == null && !rutasPublicas.matches(request)
                ? CookieSesion.leer(request)
                : Optional.empty();
        chain.doFilter(token.<HttpServletRequest>map(t -> new ConAutorizacion(request, "Bearer " + t)).orElse(request),
                response);
    }

    /** Peticion con el header Authorization agregado. */
    static final class ConAutorizacion extends HttpServletRequestWrapper {

        private final String autorizacion;

        ConAutorizacion(HttpServletRequest request, String autorizacion) {
            super(request);
            this.autorizacion = autorizacion;
        }

        @Override
        public String getHeader(String nombre) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(nombre) ? autorizacion : super.getHeader(nombre);
        }

        @Override
        public Enumeration<String> getHeaders(String nombre) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(nombre)
                    ? Collections.enumeration(List.of(autorizacion))
                    : super.getHeaders(nombre);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> nombres = new ArrayList<>(Collections.list(super.getHeaderNames()));
            nombres.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(nombres);
        }
    }
}
