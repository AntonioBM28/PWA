package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba la generacion y validacion real del JWT con las mismas llaves que usa la aplicacion.
 */
class TokenServiceTest {

    private static final String SECRETO = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTRO_SECRETO = Base64.getEncoder().encodeToString("otra-llave-de-32-bytes-0123456789".getBytes());

    private final SeguridadConfig config = new SeguridadConfig();
    private final SecretKey llave = config.llaveJwt(SECRETO);
    private final TokenService tokenService = new TokenService(config.jwtEncoder(llave), 60);
    private final JwtDecoder decoder = config.jwtDecoder(llave);

    @Test
    void generarToken_contieneUsuarioCorreoYCliente() {
        Jwt jwt = decoder.decode(tokenService.generarToken(usuario()));

        assertEquals("20", jwt.getSubject());
        assertEquals("maria.garcia@mail.com", jwt.getClaimAsString(TokenService.CLAIM_CORREO));
        assertEquals(2L, ((Number) jwt.getClaim(TokenService.CLAIM_CLIENTE_ID)).longValue());
        assertEquals(Duration.ofMinutes(60), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
        assertFalse(jwt.getClaims().containsKey("password"));
    }

    @Test
    void tokenFirmadoConOtraLlave_esRechazado() {
        SeguridadConfig otra = new SeguridadConfig();
        String tokenAjeno = new TokenService(otra.jwtEncoder(otra.llaveJwt(OTRO_SECRETO)), 60).generarToken(usuario());

        assertThrows(JwtException.class, () -> decoder.decode(tokenAjeno));
    }

    @Test
    void tokenAlterado_esRechazado() {
        String token = tokenService.generarToken(usuario());
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> decoder.decode(alterado));
    }

    @Test
    void llaveMenorA256Bits_impideIniciar() {
        String corta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThrows(IllegalStateException.class, () -> config.llaveJwt(corta));
    }

    @Test
    void generarToken_ejecutivo_sinClienteId() {
        Usuario ejecutivo = new Usuario();
        ejecutivo.setId(50);
        ejecutivo.setCorreo("ejecutivo@banco.com");
        ejecutivo.setRol(Rol.EJECUTIVO);

        Jwt jwt = decoder.decode(tokenService.generarToken(ejecutivo));

        assertEquals("EJECUTIVO", jwt.getClaimAsString(TokenService.CLAIM_ROL));
        assertFalse(jwt.hasClaim(TokenService.CLAIM_CLIENTE_ID));
    }

    @Test
    void convertidor_usuarioActivo_autenticaConElRolDeLaBaseDeDatos() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.buscarRolDeUsuarioActivo(20)).thenReturn(Optional.of(Rol.CLIENTE));
        Jwt jwt = decoder.decode(tokenService.generarToken(usuario()));

        AbstractAuthenticationToken autenticacion = new UsuarioActivoJwtConverter(repository).convert(jwt);

        assertTrue(autenticacion.isAuthenticated());
        assertEquals("maria.garcia@mail.com", autenticacion.getName());
        assertEquals(List.of("ROLE_CLIENTE"),
                autenticacion.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void convertidor_usuarioDadoDeBajaConTokenVigente_esRechazado() {
        UsuarioRepository repository = mock(UsuarioRepository.class);
        when(repository.buscarRolDeUsuarioActivo(20)).thenReturn(Optional.empty());
        Jwt jwt = decoder.decode(tokenService.generarToken(usuario()));
        UsuarioActivoJwtConverter convertidor = new UsuarioActivoJwtConverter(repository);

        assertThrows(DisabledException.class, () -> convertidor.convert(jwt));
    }

    @Test
    void convertidor_subNoNumerico_esTokenInvalidoYNoError500() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("abc").build();
        UsuarioActivoJwtConverter convertidor = new UsuarioActivoJwtConverter(mock(UsuarioRepository.class));

        assertThrows(BadCredentialsException.class, () -> convertidor.convert(jwt));
    }

    private static Usuario usuario() {
        Cliente cliente = new Cliente();
        cliente.setId(2);
        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setCliente(cliente);
        usuario.setCorreo("maria.garcia@mail.com");
        usuario.setPassword("$2a$10$hash");
        return usuario;
    }
}
