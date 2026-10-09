package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Escribe un GenericResponse de error desde un filtro. Los filtros se ejecutan antes de los
 * controllers, por eso sus errores no pasan por GlobalExceptionHandler.
 */
final class RespuestaJson {

    static final int CODIGO_ERROR = 1;

    private RespuestaJson() {
    }

    static void escribirError(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                              String mensaje) throws IOException {
        GenericResponse cuerpo = new GenericResponse();
        cuerpo.setCodigo(CODIGO_ERROR);
        cuerpo.setMensaje(mensaje);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
