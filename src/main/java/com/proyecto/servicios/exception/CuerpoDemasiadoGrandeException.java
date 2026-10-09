package com.proyecto.servicios.exception;

import java.io.IOException;

/**
 * El cuerpo de la peticion supero el tamano maximo mientras se leia (peticiones sin Content-Length).
 * Es IOException porque se lanza desde el stream que lee Jackson; GlobalExceptionHandler la detecta
 * dentro de HttpMessageNotReadableException y responde 413.
 */
public class CuerpoDemasiadoGrandeException extends IOException {

    public CuerpoDemasiadoGrandeException(long maximoBytes) {
        super("El cuerpo de la peticion excede el maximo de " + maximoBytes + " bytes");
    }
}
