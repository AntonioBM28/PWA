package com.proyecto.servicios.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Seguridad de la API: autenticacion con JWT (HS256) ligado a una sesion del servidor.
 * <ul>
 *   <li>App movil y Swagger: el token viaja en "Authorization: Bearer token".</li>
 *   <li>App web: el token viaja en la cookie HttpOnly SESION. Como el navegador la envia solo,
 *   las peticiones que modifican datos exigen ademas el header X-XSRF-TOKEN (proteccion CSRF).</li>
 * </ul>
 * Publicos: el inicio de sesion y su codigo de verificacion, el registro de clientes, la consulta
 * de codigos postales, Swagger y el catalogo de productos (RutasPublicas). Todo lo demas requiere
 * una sesion abierta de un usuario activo.
 * <p>
 * Autorizacion por rol con @PreAuthorize en los controllers (@EnableMethodSecurity): un CLIENTE
 * solo accede a sus propios datos y cuentas (ControlAcceso); un EJECUTIVO administra a todos.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    static final String ALGORITMO = "HmacSHA256";
    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UsuarioActivoJwtConverter usuarioActivoJwtConverter,
                                                   RespuestaErrorSeguridad respuestaErrorSeguridad,
                                                   @Value("${onboarding.sesion.cookie-segura}") boolean cookieSegura)
            throws Exception {
        CookieCsrfTokenRepository csrfRepositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepositorio.setCookieCustomizer(cookie -> cookie.secure(cookieSegura).sameSite("Strict").path("/"));

        return http
                .cors(Customizer.withDefaults())
                // CSRF solo aplica cuando la sesion viaja en la cookie (app web); con Bearer no hay riesgo
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CsrfTokenRepositorySinRotacion(csrfRepositorio))
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(SeguridadConfig::requiereCsrf))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                // Despues del CSRF: ver CookieSesionFilter
                .addFilterAfter(new CookieSesionFilter(RutasPublicas.MATCHER), CsrfFilter.class)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(RutasPublicas.MATCHER).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(usuarioActivoJwtConverter))
                        .authenticationEntryPoint(respuestaErrorSeguridad))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestaErrorSeguridad)
                        .accessDeniedHandler(respuestaErrorSeguridad))
                .build();
    }

    /**
     * Peticiones protegidas que modifican datos y llevan la cookie de sesion. Las rutas publicas no
     * usan la sesion, asi que una cookie vieja no debe bloquear, por ejemplo, un nuevo login.
     */
    static boolean requiereCsrf(HttpServletRequest request) {
        return !METODOS_SEGUROS.contains(request.getMethod())
                && CookieSesion.leer(request).isPresent()
                && !RutasPublicas.MATCHER.matches(request);
    }

    /**
     * Origenes que pueden llamar a la API desde el navegador con credenciales. En produccion la web
     * se sirve desde el mismo dominio y no necesita CORS; la lista es para desarrollo local
     * (flutter run -d chrome usa un puerto distinto al del backend).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${onboarding.cors.origenes-permitidos}") List<String> origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "Authorization", "X-XSRF-TOKEN", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey llaveJwt(@Value("${onboarding.jwt.secreto}") String secretoBase64) {
        byte[] llave = Base64.getDecoder().decode(secretoBase64);
        if (llave.length < 32) {
            throw new IllegalStateException("onboarding.jwt.secreto debe tener al menos 256 bits (32 bytes en Base64)");
        }
        return new SecretKeySpec(llave, ALGORITMO);
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey llaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(llaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey llaveJwt) {
        return NimbusJwtDecoder.withSecretKey(llaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
