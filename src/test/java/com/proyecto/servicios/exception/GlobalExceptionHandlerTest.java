package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void metodoNoPermitido_mensajeEnEspanol() {
        ResponseEntity<GenericResponse> respuesta = handler.handleGeneral(new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, respuesta.getStatusCode());
        assertEquals("El metodo HTTP no esta permitido en esta ruta", respuesta.getBody().getMensaje());
    }

    @Test
    void tipoDeContenidoNoSoportado_mensajeEnEspanol() {
        ResponseEntity<GenericResponse> respuesta = handler.handleGeneral(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, respuesta.getStatusCode());
        assertEquals("Tipo de contenido no soportado; envie application/json", respuesta.getBody().getMensaje());
    }

    @Test
    void rutaInexistente_noRevelaDetallesInternos() {
        ResponseEntity<GenericResponse> respuesta = handler.handleGeneral(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "no-existe"));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("El recurso solicitado no existe", respuesta.getBody().getMensaje());
    }

    @Test
    void errorInesperado_respondeMensajeGenericoSinElDetalle() {
        ResponseEntity<GenericResponse> respuesta = handler.handleGeneral(
                new IllegalStateException("detalle interno: tabla onboarding.clientes"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals(GlobalExceptionHandler.MENSAJE_ERROR_INTERNO, respuesta.getBody().getMensaje());
        assertFalse(respuesta.getBody().getMensaje().contains("onboarding"));
    }

    @Test
    void cuerpoDemasiadoGrandeDuranteLaLectura_respondePayloadTooLarge() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("I/O error",
                new CuerpoDemasiadoGrandeException(65536), new MockHttpInputMessage(new byte[0]));

        ResponseEntity<GenericResponse> respuesta = handler.handleCuerpoIlegible(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, respuesta.getStatusCode());
    }

    @Test
    void accesoDenegado_responde403() {
        ResponseEntity<GenericResponse> respuesta = handler.handleAccesoDenegado(new AccessDeniedException("x"));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
    }

    @Test
    void demasiadosIntentos_incluyeRetryAfter() {
        ResponseEntity<GenericResponse> respuesta = handler.handleDemasiadosIntentos(
                new DemasiadosIntentosException("Demasiados intentos", 120));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, respuesta.getStatusCode());
        assertEquals("120", respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }
}
