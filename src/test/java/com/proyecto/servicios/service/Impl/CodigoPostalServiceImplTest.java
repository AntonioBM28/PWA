package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.CodigoPostalClient;
import com.proyecto.servicios.exception.CodigoPostalException;
import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.model.codigopostal.AsentamientoApi;
import com.proyecto.servicios.model.codigopostal.CodigoPostalApiResponse;
import feign.Request;
import feign.RetryableException;
import feign.codec.DecodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoPostalServiceImplTest {

    private static final String CP = "37806";

    @Mock
    private CodigoPostalClient codigoPostalClient;

    private CodigoPostalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CodigoPostalServiceImpl(codigoPostalClient);
    }

    @Test
    void consultarCodigoPostal_respuestaExitosa_devuelveEstadoMunicipioYColonias() {
        when(codigoPostalClient.consultar(CP)).thenReturn(respuesta(List.of("15 de Septiembre", "Dos Plazas")));

        CodigoPostalResponse response = service.consultarCodigoPostal(CP);

        assertEquals(CodigoPostalServiceImpl.CODIGO_EXITO, response.getCodigo());
        assertEquals(CP, response.getCodigoPostal());
        assertEquals("Guanajuato", response.getEstado());
        assertEquals("Dolores Hidalgo", response.getMunicipio());
        assertEquals(List.of("15 de Septiembre", "Dos Plazas"), response.getColonias());
    }

    @Test
    void consultarCodigoPostal_noExiste_propagaNotFound() {
        when(codigoPostalClient.consultar(CP))
                .thenThrow(new CodigoPostalException("El codigo postal no existe", HttpStatus.NOT_FOUND));

        CodigoPostalException ex = assertThrows(CodigoPostalException.class, () -> service.consultarCodigoPostal(CP));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void consultarCodigoPostal_sinColonias_lanzaNotFound() {
        when(codigoPostalClient.consultar(CP)).thenReturn(respuesta(Collections.emptyList()));

        assertStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void consultarCodigoPostal_respuestaNula_lanzaNotFound() {
        when(codigoPostalClient.consultar(CP)).thenReturn(null);

        assertStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void consultarCodigoPostal_timeout_lanzaGatewayTimeout() {
        when(codigoPostalClient.consultar(CP))
                .thenThrow(retryable(new SocketTimeoutException("Read timed out")));

        assertStatus(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void consultarCodigoPostal_errorDeConexion_lanzaServiceUnavailable() {
        when(codigoPostalClient.consultar(CP))
                .thenThrow(retryable(new ConnectException("Connection refused")));

        assertStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void consultarCodigoPostal_jsonIlegible_lanzaBadGateway() {
        when(codigoPostalClient.consultar(CP))
                .thenThrow(new DecodeException(200, "JSON invalido", request()));

        assertStatus(HttpStatus.BAD_GATEWAY);
    }

    private void assertStatus(HttpStatus esperado) {
        CodigoPostalException ex = assertThrows(CodigoPostalException.class, () -> service.consultarCodigoPostal(CP));
        assertEquals(esperado, ex.getHttpStatus());
    }

    private static CodigoPostalApiResponse respuesta(List<String> colonias) {
        CodigoPostalApiResponse respuesta = new CodigoPostalApiResponse();
        respuesta.setCp(CP);
        respuesta.setEstado("Guanajuato");
        respuesta.setMunicipio("Dolores Hidalgo");
        respuesta.setAsentamientos(colonias.stream().map(nombre -> {
            AsentamientoApi asentamiento = new AsentamientoApi();
            asentamiento.setNombre(nombre);
            return asentamiento;
        }).toList());
        return respuesta;
    }

    private static RetryableException retryable(Exception causa) {
        return new RetryableException(-1, causa.getMessage(), Request.HttpMethod.GET, causa, (Long) null, request());
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "/cp/" + CP, Collections.emptyMap(), null,
                StandardCharsets.UTF_8, null);
    }
}
