package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.CodigoPostalClient;
import com.proyecto.servicios.config.CacheConfig;
import com.proyecto.servicios.exception.CodigoPostalException;
import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.model.codigopostal.AsentamientoApi;
import com.proyecto.servicios.model.codigopostal.CodigoPostalApiResponse;
import com.proyecto.servicios.service.CodigoPostalService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Consulta estado, municipio y colonias de un codigo postal. El resultado se guarda en cache,
 * asi la consulta del front y la validacion al registrar el cliente no repiten la llamada.
 * Los errores no se guardan en cache.
 */
@Service
@Slf4j
public class CodigoPostalServiceImpl implements CodigoPostalService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";

    private final CodigoPostalClient codigoPostalClient;

    public CodigoPostalServiceImpl(CodigoPostalClient codigoPostalClient) {
        this.codigoPostalClient = codigoPostalClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CODIGOS_POSTALES)
    public CodigoPostalResponse consultarCodigoPostal(String codigoPostal) {
        log.info("Inicio consulta de codigo postal {}", codigoPostal);
        long inicio = System.currentTimeMillis();
        try {
            CodigoPostalApiResponse respuesta = invocarServicio(codigoPostal);
            return toResponse(codigoPostal, validarRespuesta(respuesta));
        } catch (CodigoPostalException e) {
            log.error("Fallo consulta de codigo postal {}: estatus={}, detalle={}",
                    codigoPostal, e.getHttpStatus().value(), e.getMessage());
            throw e;
        } finally {
            log.info("Fin consulta de codigo postal {} en {} ms", codigoPostal, System.currentTimeMillis() - inicio);
        }
    }

    /**
     * Las respuestas HTTP no exitosas las traduce CodigoPostalFeignConfig; aqui se traducen los
     * errores de red (timeouts y comunicacion) y de lectura del JSON.
     */
    private CodigoPostalApiResponse invocarServicio(String codigoPostal) {
        try {
            return codigoPostalClient.consultar(codigoPostal);
        } catch (RetryableException e) {
            if (ExceptionUtils.indexOfType(e, SocketTimeoutException.class) >= 0) {
                throw new CodigoPostalException("Tiempo de espera agotado al consultar el codigo postal",
                        HttpStatus.GATEWAY_TIMEOUT, e);
            }
            throw new CodigoPostalException("No fue posible comunicarse con el servicio de codigos postales",
                    HttpStatus.SERVICE_UNAVAILABLE, e);
        } catch (FeignException e) {
            throw new CodigoPostalException("El servicio de codigos postales devolvio una respuesta invalida",
                    HttpStatus.BAD_GATEWAY, e);
        }
    }

    private CodigoPostalApiResponse validarRespuesta(CodigoPostalApiResponse respuesta) {
        if (respuesta == null || respuesta.getAsentamientos() == null || respuesta.getAsentamientos().isEmpty()) {
            throw new CodigoPostalException("El codigo postal no existe", HttpStatus.NOT_FOUND);
        }
        return respuesta;
    }

    private CodigoPostalResponse toResponse(String codigoPostal, CodigoPostalApiResponse respuesta) {
        List<String> colonias = respuesta.getAsentamientos().stream()
                .map(AsentamientoApi::getNombre)
                .toList();
        CodigoPostalResponse response = new CodigoPostalResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setCodigoPostal(codigoPostal);
        response.setEstado(respuesta.getEstado());
        response.setMunicipio(respuesta.getMunicipio());
        response.setColonias(colonias);
        return response;
    }
}
