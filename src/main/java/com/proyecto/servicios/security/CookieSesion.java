package com.proyecto.servicios.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie de sesion de la app web. Es HttpOnly (JavaScript no puede leerla, asi un ataque XSS no
 * roba el token), Secure (solo viaja por HTTPS; los navegadores la aceptan tambien en localhost) y
 * SameSite=Strict (el navegador no la envia en peticiones iniciadas desde otros sitios).
 */
@Component
public class CookieSesion {

    public static final String NOMBRE = "SESION";

    private final boolean segura;

    public CookieSesion(@Value("${onboarding.sesion.cookie-segura}") boolean segura) {
        this.segura = segura;
    }

    public ResponseCookie crear(String token, Duration vigencia) {
        return base(token).maxAge(vigencia).build();
    }

    /** Cookie vacia con Max-Age=0: el navegador la borra. */
    public ResponseCookie borrar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public static Optional<String> leer(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NOMBRE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valor -> !valor.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path("/");
    }
}
