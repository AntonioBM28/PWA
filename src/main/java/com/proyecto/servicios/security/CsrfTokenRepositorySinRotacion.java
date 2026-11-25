package com.proyecto.servicios.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Repositorio del token CSRF (cookie XSRF-TOKEN) que no lo borra al autenticar.
 * <p>
 * Spring Security borra el token CSRF cada vez que autentica una peticion, pensando en un
 * login con sesion de servidor. Aqui la autenticacion es sin estado: se repite en CADA
 * peticion, asi que la cookie se borraba en una respuesta y se volvia a crear en la
 * siguiente. La app web, que copia la cookie al header X-XSRF-TOKEN, se quedaba sin token
 * justo despues de recargar la pagina y su siguiente POST recibia 403.
 * <p>
 * Conservar el token no debilita la proteccion: sigue siendo aleatorio por navegador, solo
 * lo puede leer el propio sitio (SameSite=Strict) y la sesion vive en el servidor.
 */
public class CsrfTokenRepositorySinRotacion implements CsrfTokenRepository {

    private final CsrfTokenRepository delegado;

    public CsrfTokenRepositorySinRotacion(CsrfTokenRepository delegado) {
        this.delegado = delegado;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegado.generateToken(request);
    }

    /** Un token nulo es la orden de borrarlo: se ignora (ver la descripcion de la clase). */
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        if (token != null) {
            delegado.saveToken(token, request, response);
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegado.loadToken(request);
    }
}
