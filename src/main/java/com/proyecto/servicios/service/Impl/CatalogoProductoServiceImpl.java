package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductosClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.exception.GestoPagoResponseException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.CatalogoProductoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CatalogoProductoServiceImpl implements CatalogoProductoService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GestoPagoProductosClient productosClient;
    private final GestoPagoTokenService tokenService;
    private final CatProductoRepository catProductoRepository;
    private final GestoPagoProductoMapper productoMapper;
    private final String tokenConfigurado;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;

    public CatalogoProductoServiceImpl(GestoPagoProductosClient productosClient,
                                       GestoPagoTokenService tokenService,
                                       CatProductoRepository catProductoRepository,
                                       GestoPagoProductoMapper productoMapper,
                                       @Value("${gestopago.productos.token:}") String tokenConfigurado,
                                       @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
                                       @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo) {
        this.productosClient = productosClient;
        this.tokenService = tokenService;
        this.catProductoRepository = catProductoRepository;
        this.productoMapper = productoMapper;
        this.tokenConfigurado = tokenConfigurado;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
    }

    @Override
    public int sincronizarCatalogo() {
        List<GestoPagoProducto> productos = consultarGestoPago();
        guardarCatalogo(productos);
        return productos.size();
    }

    private List<GestoPagoProducto> consultarGestoPago() {
        log.info("Inicio invocacion GestoPago getProductList");
        long inicio = System.currentTimeMillis();
        try {
            GestoPagoProductListResponse respuesta = invocarServicio(BEARER_PREFIX + resolverToken());
            List<GestoPagoProducto> productos = validarRespuesta(respuesta);
            log.info("GestoPago getProductList devolvio {} productos", productos.size());
            return productos;
        } catch (GestoPagoIntegrationException e) {
            log.error("Fallo invocacion GestoPago getProductList: tipo={}, detalle={}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            log.info("Fin invocacion GestoPago getProductList en {} ms", System.currentTimeMillis() - inicio);
        }
    }

    /**
     * Prioriza el token definido en configuracion; si no existe, usa el token vigente que
     * GestoPagoTokenService renueva periodicamente. Si aun no hay uno (p. ej. al arrancar),
     * solicita su renovacion una vez.
     */
    private String resolverToken() {
        if (StringUtils.isNotBlank(tokenConfigurado)) {
            return tokenConfigurado;
        }
        return buscarTokenVigente()
                .or(() -> {
                    log.warn("No hay token GestoPago vigente, se solicita su renovacion");
                    tokenService.renovarToken();
                    return buscarTokenVigente();
                })
                .orElseThrow(() -> new GestoPagoAuthenticationException("No existe un token de GestoPago configurado o vigente"));
    }

    private Optional<String> buscarTokenVigente() {
        return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .filter(token -> Boolean.TRUE.equals(token.getActivo()))
                .map(GestoPagoToken::getToken)
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Las respuestas HTTP no exitosas las traduce GestoPagoProductosFeignConfig; aqui se
     * traducen los errores de red (timeouts y comunicacion) y de lectura del XML.
     */
    private GestoPagoProductListResponse invocarServicio(String authorization) {
        try {
            return productosClient.getProductList(authorization);
        } catch (RetryableException e) {
            if (ExceptionUtils.indexOfType(e, SocketTimeoutException.class) >= 0) {
                throw new GestoPagoTimeoutException("Tiempo de espera agotado al invocar GestoPago", e);
            }
            throw new GestoPagoCommunicationException("No fue posible comunicarse con GestoPago", e);
        } catch (FeignException e) {
            throw new GestoPagoResponseException("GestoPago devolvio una respuesta invalida", e.status());
        }
    }

    private List<GestoPagoProducto> validarRespuesta(GestoPagoProductListResponse respuesta) {
        if (respuesta == null || respuesta.getMensaje() == null) {
            throw new GestoPagoResponseException("GestoPago devolvio una respuesta vacia", null);
        }
        if (!respuesta.getMensaje().isExitoso()) {
            throw new GestoPagoResponseException(
                    "GestoPago no completo la operacion, codigo=" + respuesta.getMensaje().getCodigo(), null);
        }
        if (respuesta.getProductos() == null || respuesta.getProductos().isEmpty()) {
            // Se evita borrar el catalogo vigente por una respuesta sin productos.
            throw new GestoPagoResponseException("GestoPago devolvio un catalogo sin productos", null);
        }
        return respuesta.getProductos();
    }

    private void guardarCatalogo(List<GestoPagoProducto> productos) {
        Instant fechaActualizacion = Instant.now();
        List<CatProducto> documentos = productos.stream()
                .map(producto -> productoMapper.toDocument(producto, fechaActualizacion))
                .toList();
        try {
            catProductoRepository.reemplazarCatalogo(documentos);
            log.info("Catalogo de productos guardado en MongoDB: {} productos", documentos.size());
        } catch (DataAccessException e) {
            log.error("Fallo al guardar el catalogo en MongoDB: tipo={}", e.getClass().getSimpleName());
            throw new CatalogoProductosException("No fue posible guardar el catalogo de productos", e);
        }
    }
}
