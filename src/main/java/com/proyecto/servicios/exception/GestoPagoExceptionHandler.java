package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GestoPagoExceptionHandler {

    static final int CODIGO_ERROR = 1;

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleIntegration(GestoPagoIntegrationException ex) {
        return error(ex.getMessage(), ex.getHttpStatus());
    }

    @ExceptionHandler(CatalogoProductosException.class)
    public ResponseEntity<GenericResponse> handleCatalogo(CatalogoProductosException ex) {
        return error(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static ResponseEntity<GenericResponse> error(String mensaje, HttpStatus status) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(CODIGO_ERROR);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
