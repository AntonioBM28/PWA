package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.onboarding.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Genera el JWT de un usuario autenticado. El token lleva el id del usuario (sub), su correo, su
 * rol y, si es CLIENTE, el id de su cliente; nunca la contrasena. Los permisos no se toman del rol
 * del token sino de la base de datos (UsuarioActivoJwtConverter).
 */
@Service
public class TokenService {

    public static final String CLAIM_CORREO = "correo";
    public static final String CLAIM_CLIENTE_ID = "clienteId";
    public static final String CLAIM_ROL = "rol";
    static final String EMISOR = "servicios-onboarding";

    private final JwtEncoder jwtEncoder;
    private final Duration vigencia;

    public TokenService(JwtEncoder jwtEncoder, @Value("${onboarding.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.vigencia = Duration.ofMinutes(expiracionMinutos);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vigencia))
                .claim(CLAIM_CORREO, usuario.getCorreo())
                .claim(CLAIM_ROL, usuario.getRol().name());
        // Un EJECUTIVO no es cliente, por eso no lleva clienteId
        if (usuario.getCliente() != null) {
            claims.claim(CLAIM_CLIENTE_ID, usuario.getCliente().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    public long getVigenciaSegundos() {
        return vigencia.toSeconds();
    }
}
