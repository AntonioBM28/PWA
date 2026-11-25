package com.proyecto.servicios.security;

import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.SesionService;
import com.proyecto.servicios.service.SesionService.EstadoSesion;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Convierte un JWT ya validado (firma y vigencia) en la autenticacion de la peticion:
 * <ul>
 *   <li>Verifica que la sesion del token (claim jti) siga abierta en el servidor: tras un logout, o
 *   si el usuario inicio sesion en otro dispositivo, el token deja de servir de inmediato.</li>
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
    private final SesionService sesionService;

    public UsuarioActivoJwtConverter(UsuarioRepository usuarioRepository, SesionService sesionService) {
        this.usuarioRepository = usuarioRepository;
        this.sesionService = sesionService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer usuarioId = usuarioId(jwt);
        EstadoSesion estado = sesionService.estado(sesionId(jwt), usuarioId);
        if (estado == EstadoSesion.REEMPLAZADA) {
            throw new SesionCerradaException(RespuestaErrorSeguridad.MENSAJE_SESION_REEMPLAZADA);
        }
        if (estado != EstadoSesion.ABIERTA) {
            throw new SesionCerradaException(RespuestaErrorSeguridad.MENSAJE_SESION_CERRADA);
        }
        Rol rol = usuarioRepository.buscarRolDeUsuarioActivo(usuarioId)
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

    /** Los tokens emitidos antes de las sesiones en servidor no traen jti y ya no se aceptan. */
    private static UUID sesionId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getId());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException(RespuestaErrorSeguridad.MENSAJE_TOKEN_INVALIDO, e);
        }
    }
}
