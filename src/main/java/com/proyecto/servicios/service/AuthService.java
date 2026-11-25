package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.ReenviarCodigoRequest;
import com.proyecto.servicios.model.onboarding.SesionResponse;
import com.proyecto.servicios.model.onboarding.VerificarCodigoRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Inicio de sesion en dos pasos: contrasena y codigo enviado por correo (2FA). Un usuario solo
 * puede tener una sesion abierta a la vez.
 */
public interface AuthService {

    /** Paso 1: valida la contrasena y envia el codigo de verificacion por correo. */
    DesafioVerificacionResponse iniciarSesion(LoginRequest request);

    /** Paso 2: valida el codigo y abre la sesion. */
    SesionIniciada verificarCodigo(VerificarCodigoRequest request);

    /** Envia un codigo nuevo para el mismo desafio (con espera y limite de reenvios). */
    DesafioVerificacionResponse reenviarCodigo(ReenviarCodigoRequest request);

    /** Datos de la sesion actual (la usa la app web al recargar la pagina). */
    SesionResponse consultarSesion(JwtAuthenticationToken autenticacion);

    /** Cierra la sesion en el servidor: el token deja de funcionar de inmediato. */
    void cerrarSesion(JwtAuthenticationToken autenticacion);
}
