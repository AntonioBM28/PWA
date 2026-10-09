package com.proyecto.servicios.exception;

/**
 * No existe un usuario con el correo indicado en el inicio de sesion. Responde igual que
 * CredencialesInvalidasException (401, mismo mensaje) para que el login no permita averiguar que
 * correos estan registrados; la diferencia solo queda en el tipo de excepcion y en el log.
 */
public class UsuarioNoEncontradoException extends CredencialesInvalidasException {
}
