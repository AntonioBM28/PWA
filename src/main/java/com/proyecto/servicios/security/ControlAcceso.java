package com.proyecto.servicios.security;

import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Reglas de propiedad que usan las anotaciones @PreAuthorize de los controllers, por ejemplo
 * {@code @PreAuthorize("hasRole('EJECUTIVO') or @acceso.esCliente(#id)")}.
 * <p>
 * Un CLIENTE solo puede acceder a sus propios datos: se compara el recurso con el clienteId de su
 * token. Un EJECUTIVO no tiene clienteId, por eso estas reglas siempre son falsas para el y su
 * acceso se concede con hasRole('EJECUTIVO').
 */
@Component("acceso")
public class ControlAcceso {

    private final CuentaRepository cuentaRepository;

    public ControlAcceso(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    /** true si el usuario autenticado es el cliente indicado. */
    public boolean esCliente(Integer clienteId) {
        Integer propio = clienteIdActual();
        return propio != null && propio.equals(clienteId);
    }

    /** true si la cuenta pertenece al cliente autenticado. */
    public boolean esCuentaPropia(String numeroCuenta) {
        Integer propio = clienteIdActual();
        return propio != null && cuentaRepository.existsByNumeroCuentaAndClienteId(numeroCuenta, propio);
    }

    private static Integer clienteIdActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken jwt)) {
            return null;
        }
        Object clienteId = jwt.getToken().getClaim(TokenService.CLAIM_CLIENTE_ID);
        return clienteId instanceof Number numero ? numero.intValue() : null;
    }
}
