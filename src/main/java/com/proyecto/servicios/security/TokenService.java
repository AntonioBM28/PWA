package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Genera el JWT de un usuario autenticado. El token lleva el id del usuario (sub), el id de su
 * sesion (jti), su correo, su rol y, si es CLIENTE, el id de su cliente; nunca la contrasena.
 * <p>
 * El token vence junto con su sesion. Cada peticion revisa en la base de datos que la sesion siga
 * abierta (UsuarioActivoJwtConverter), asi el servidor puede invalidar un token antes de que expire.
 */
@Service
public class TokenService {

    public static final String CLAIM_CORREO = "correo";
    public static final String CLAIM_CLIENTE_ID = "clienteId";
    public static final String CLAIM_ROL = "rol";
    static final String EMISOR = "servicios-onboarding";

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generarToken(Usuario usuario, Sesion sesion) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(sesion.getId().toString())
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(sesion.getFechaCreacion().toInstant())
                .expiresAt(sesion.getFechaExpiracion().toInstant())
                .claim(CLAIM_CORREO, usuario.getCorreo())
                .claim(CLAIM_ROL, usuario.getRol().name());
        // Un EJECUTIVO no es cliente, por eso no lleva clienteId
        if (usuario.getCliente() != null) {
            claims.claim(CLAIM_CLIENTE_ID, usuario.getCliente().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
