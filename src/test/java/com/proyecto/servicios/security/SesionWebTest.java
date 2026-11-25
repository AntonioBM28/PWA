package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Sesion de la app web: cookie HttpOnly, lectura del token, CSRF y respuestas 401/403. */
class SesionWebTest {

    private final CookieSesion cookieSesion = new CookieSesion(true);
    private final CookieSesionFilter cookieFilter = new CookieSesionFilter(RutasPublicas.MATCHER);
    private final RespuestaErrorSeguridad respuestaError = new RespuestaErrorSeguridad(new ObjectMapper(), cookieSesion);

    @Test
    void cookieDeSesion_esHttpOnlySecureYSameSiteStrict() {
        ResponseCookie cookie = cookieSesion.crear("jwt", Duration.ofHours(1));

        assertEquals(CookieSesion.NOMBRE, cookie.getName());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertEquals(Duration.ofHours(1), cookie.getMaxAge());
        assertEquals(Duration.ZERO, cookieSesion.borrar().getMaxAge());
    }

    @Test
    void cookieFilter_respetaElHeaderBearerDeLaAppMovil() throws Exception {
        MockHttpServletRequest request = peticion("GET", "/auth/sesion");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-header");
        request.setCookies(new Cookie(CookieSesion.NOMBRE, "token-cookie"));

        assertEquals("Bearer token-header", autorizacionTrasElFiltro(request));
    }

    @Test
    void cookieFilter_copiaLaCookieAlHeaderEnRutasProtegidas() throws Exception {
        MockHttpServletRequest request = peticion("POST", "/cuentas/saldo");
        request.setCookies(new Cookie(CookieSesion.NOMBRE, "token-cookie"));

        assertEquals("Bearer token-cookie", autorizacionTrasElFiltro(request));
    }

    @Test
    void cookieFilter_ignoraLaCookieEnRutasPublicas_paraPoderIniciarSesionDeNuevo() throws Exception {
        MockHttpServletRequest request = peticion("POST", RutasPublicas.LOGIN);
        request.setCookies(new Cookie(CookieSesion.NOMBRE, "token-de-sesion-cerrada"));

        assertNull(autorizacionTrasElFiltro(request));
    }

    @Test
    void csrf_soloSeExigeEnPeticionesProtegidasQueModificanYLlevanLaCookie() {
        MockHttpServletRequest loginConCookieVieja = peticion("POST", RutasPublicas.LOGIN);
        loginConCookieVieja.setCookies(new Cookie(CookieSesion.NOMBRE, "jwt-viejo"));
        assertFalse(SeguridadConfig.requiereCsrf(loginConCookieVieja));

        MockHttpServletRequest postConCookie = peticion("POST", "/cuentas/saldo");
        postConCookie.setCookies(new Cookie(CookieSesion.NOMBRE, "jwt"));
        MockHttpServletRequest getConCookie = peticion("GET", "/auth/sesion");
        getConCookie.setCookies(new Cookie(CookieSesion.NOMBRE, "jwt"));
        MockHttpServletRequest postConBearer = peticion("POST", "/cuentas/saldo");
        postConBearer.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt");

        assertTrue(SeguridadConfig.requiereCsrf(postConCookie));
        assertFalse(SeguridadConfig.requiereCsrf(getConCookie));
        assertFalse(SeguridadConfig.requiereCsrf(postConBearer));
    }

    @Test
    void repositorioCsrf_noBorraElTokenAlAutenticar_peroSiGuardaUnoNuevo() {
        org.springframework.security.web.csrf.CsrfTokenRepository delegado =
                mock(org.springframework.security.web.csrf.CsrfTokenRepository.class);
        CsrfTokenRepositorySinRotacion repositorio = new CsrfTokenRepositorySinRotacion(delegado);
        MockHttpServletRequest request = peticion("GET", "/auth/sesion");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "valor");

        repositorio.saveToken(null, request, response);
        repositorio.saveToken(token, request, response);
        repositorio.loadToken(request);
        repositorio.generateToken(request);

        verify(delegado, org.mockito.Mockito.never()).saveToken(null, request, response);
        verify(delegado).saveToken(token, request, response);
        verify(delegado).loadToken(request);
        verify(delegado).generateToken(request);
    }

    @Test
    void csrfCookieFilter_generaElTokenParaQueSeEscribaLaCookie() throws Exception {
        MockHttpServletRequest request = peticion("GET", "/auth/sesion");
        boolean[] solicitado = {false};
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "valor");
        Supplier<CsrfToken> diferido = () -> {
            solicitado[0] = true;
            return token;
        };
        request.setAttribute(CsrfToken.class.getName(), (CsrfToken) new CsrfTokenDiferido(diferido));
        FilterChain cadena = mock(FilterChain.class);

        new CsrfCookieFilter().doFilter(request, new MockHttpServletResponse(), cadena);

        assertTrue(solicitado[0]);
        verify(cadena).doFilter(eq(request), any());
    }

    @Test
    void respuesta401_borraLaCookieYExplicaElMotivo() throws Exception {
        MockHttpServletRequest request = peticion("GET", "/auth/sesion");
        request.setCookies(new Cookie(CookieSesion.NOMBRE, "jwt"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        respuestaError.commence(request, response,
                new SesionCerradaException(RespuestaErrorSeguridad.MENSAJE_SESION_REEMPLAZADA));

        assertEquals(401, response.getStatus());
        assertTrue(response.getHeader(HttpHeaders.SET_COOKIE).startsWith(CookieSesion.NOMBRE + "=;"));
        assertTrue(response.getContentAsString().contains("otro dispositivo"));
    }

    @Test
    void respuesta401_sinCookieNoLaBorra() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        respuestaError.commence(peticion("GET", "/auth/sesion"), response,
                new InsufficientAuthenticationException("sin token"));

        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        assertTrue(response.getContentAsString().contains(RespuestaErrorSeguridad.MENSAJE_SIN_TOKEN));
    }

    @Test
    void respuesta403_distingueCsrfDePermisos() throws Exception {
        MockHttpServletResponse csrf = new MockHttpServletResponse();
        MockHttpServletResponse permiso = new MockHttpServletResponse();

        respuestaError.handle(peticion("POST", "/x"), csrf, new MissingCsrfTokenException("t"));
        respuestaError.handle(peticion("POST", "/x"), permiso, new AccessDeniedException("x"));

        assertTrue(csrf.getContentAsString().contains("token de seguridad"));
        assertTrue(permiso.getContentAsString().contains(RespuestaErrorSeguridad.MENSAJE_ACCESO_DENEGADO));
    }

    /** Header Authorization que ve el resto de la cadena despues de CookieSesionFilter. */
    private String autorizacionTrasElFiltro(MockHttpServletRequest request) throws Exception {
        String[] visto = new String[1];
        cookieFilter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> visto[0] = ((jakarta.servlet.http.HttpServletRequest) req).getHeader(HttpHeaders.AUTHORIZATION));
        return visto[0];
    }

    private static MockHttpServletRequest peticion(String metodo, String ruta) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setServletPath(ruta);
        return request;
    }

    /** CsrfToken que solo calcula su valor al pedirlo, como el de Spring Security. */
    private record CsrfTokenDiferido(Supplier<CsrfToken> origen) implements CsrfToken {
        @Override
        public String getHeaderName() {
            return "X-XSRF-TOKEN";
        }

        @Override
        public String getParameterName() {
            return "_csrf";
        }

        @Override
        public String getToken() {
            return origen.get().getToken();
        }
    }
}
