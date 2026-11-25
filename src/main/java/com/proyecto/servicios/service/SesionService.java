package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;

import java.util.UUID;

/** Sesiones respaldadas por el servidor: un usuario tiene como maximo una sesion abierta. */
public interface SesionService {

    /**
     * Verifica, antes de enviar el codigo, que el usuario no tenga una sesion abierta en otro
     * dispositivo, salvo que ya haya aceptado cerrarla.
     *
     * @throws com.proyecto.servicios.exception.SesionActivaException si la tiene
     */
    void validarDisponible(Integer usuarioId, String dispositivo, boolean cerrarSesionAnterior);

    /** Abre la sesion tras verificar el codigo; cierra la anterior si corresponde. */
    Sesion abrir(Usuario usuario, Plataforma plataforma, String dispositivo, boolean cerrarSesionAnterior);

    /** Estado con el que se valida cada peticion autenticada. */
    EstadoSesion estado(UUID sesionId, Integer usuarioId);

    /** Cierre voluntario. Cerrar una sesion ya cerrada no hace nada. */
    void cerrar(UUID sesionId, Integer usuarioId);

    enum EstadoSesion {
        ABIERTA,
        /** Se cerro porque el usuario inicio sesion en otro dispositivo. */
        REEMPLAZADA,
        /** Cerrada por logout, expirada o inexistente. */
        INVALIDA
    }
}
