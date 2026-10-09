package com.proyecto.servicios.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Seguridad de la API: autenticacion sin estado con JWT (HS256) enviado como
 * "Authorization: Bearer token".
 * <p>
 * Publicos: el login, el registro de clientes (el usuario se crea al registrarse), la consulta de
 * codigos postales (la usa el formulario de registro), Swagger y los modulos que ya existian
 * (catalogo de productos). Todo lo demas requiere un token valido de un usuario activo.
 * <p>
 * Autorizacion por rol con @PreAuthorize en los controllers (@EnableMethodSecurity): un CLIENTE
 * solo accede a sus propios datos y cuentas (ControlAcceso); un EJECUTIVO administra a todos.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    static final String ALGORITMO = "HmacSHA256";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UsuarioActivoJwtConverter usuarioActivoJwtConverter,
                                                   RespuestaErrorSeguridad respuestaErrorSeguridad) throws Exception {
        return http
                // API sin sesiones ni formularios: CSRF no aplica porque el token no viaja en una cookie
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/clientes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/codigos-postales/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/productos").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(usuarioActivoJwtConverter))
                        .authenticationEntryPoint(respuestaErrorSeguridad))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestaErrorSeguridad)
                        .accessDeniedHandler(respuestaErrorSeguridad))
                .build();
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
