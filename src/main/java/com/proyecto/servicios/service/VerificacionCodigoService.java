package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;

import java.util.UUID;

/**
 * Segundo factor por correo. Se valida en el servidor: el frontend nunca conoce el codigo, solo el
 * usuario que recibe el correo.
 */
public interface VerificacionCodigoService {

    /** Genera un codigo, guarda su hash y lo envia al correo del usuario. */
    DesafioVerificacionResponse crearYEnviar(Usuario usuario, Plataforma plataforma, String dispositivo,
                                             boolean cerrarSesionAnterior);

    /**
     * Comprueba el codigo y lo marca como usado (un solo uso).
     *
     * @throws com.proyecto.servicios.exception.CodigoVerificacionException si es incorrecto, expiro,
     *                                                                       ya se uso o supero los intentos
     */
    CodigoVerificacion validar(UUID desafioId, String codigo);

    /** Genera y envia un codigo nuevo para el mismo desafio. */
    DesafioVerificacionResponse reenviar(UUID desafioId);
}
