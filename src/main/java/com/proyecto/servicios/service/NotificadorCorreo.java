package com.proyecto.servicios.service;

import java.time.Duration;

/** Envio de correos al usuario. */
public interface NotificadorCorreo {

    /**
     * Envia el codigo de verificacion del inicio de sesion.
     *
     * @throws com.proyecto.servicios.exception.CorreoNoEnviadoException si el servidor de correo falla
     */
    void enviarCodigoVerificacion(String correo, String codigo, Duration vigencia);
}
