package com.proyecto.servicios.security;

import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte un JWT ya validado (firma y vigencia) en la autenticacion de la peticion:
 * <ul>
 *   <li>Verifica que el usuario siga activo: si el cliente se dio de baja despues de obtener el
 *   token, el token deja de servir de inmediato y no hasta que expire.</li>
 *   <li>Asigna el rol (ROLE_CLIENTE o ROLE_EJECUTIVO) leido de la base de datos en esa misma
 *   consulta, para que un cambio de rol aplique de inmediato.</li>
 * </ul>
 */
@Component
public class UsuarioActivoJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String PREFIJO_ROL = "ROLE_";

    private final UsuarioRepository usuarioRepository;

    public UsuarioActivoJwtConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Rol rol = usuarioRepository.buscarRolDeUsuarioActivo(usuarioId(jwt))
                .orElseThrow(() -> new DisabledException(RespuestaErrorSeguridad.MENSAJE_USUARIO_INACTIVO));
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(PREFIJO_ROL + rol.name())),
                jwt.getClaimAsString(TokenService.CLAIM_CORREO));
    }

    private static Integer usuarioId(Jwt jwt) {
        try {
            return Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new BadCredentialsException(RespuestaErrorSeguridad.MENSAJE_TOKEN_INVALIDO, e);
        }
    }
}
