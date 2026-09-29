package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.ProductosResponse;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Consulta el catalogo desde MongoDB; la carga desde GestoPago la hace CatalogoProductoService.
 */
@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    private static final Sort ORDEN_CATALOGO = Sort.by("servicio", "nombre");

    private final CatProductoRepository catProductoRepository;
    private final GestoPagoProductoMapper productoMapper;

    public ProductoServiceImpl(CatProductoRepository catProductoRepository, GestoPagoProductoMapper productoMapper) {
        this.catProductoRepository = catProductoRepository;
        this.productoMapper = productoMapper;
    }

    @Override
    public ProductosResponse obtenerProductos() {
        try {
            ProductosResponse response = new ProductosResponse();
            response.setCodigo(CODIGO_EXITO);
            response.setMensaje(MENSAJE_EXITO);
            response.setProductos(productoMapper.toResponseList(catProductoRepository.findAll(ORDEN_CATALOGO)));
            return response;
        } catch (DataAccessException e) {
            log.error("Fallo al consultar el catalogo en MongoDB: tipo={}", e.getClass().getSimpleName());
            throw new CatalogoProductosException("El catalogo de productos no esta disponible", e);
        }
    }
}
