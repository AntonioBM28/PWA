package com.proyecto.servicios.exception;

/**
 * Error al leer o guardar el catalogo de productos en MongoDB.
 */
public class CatalogoProductosException extends RuntimeException {

    public CatalogoProductosException(String message, Throwable cause) {
        super(message, cause);
    }
}
