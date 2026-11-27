package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.enums.OrdenProducto;
import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.CatalogoProductosResponse;
import com.proyecto.servicios.model.ProductoFiltro;
import com.proyecto.servicios.model.ProductoResponse;
import com.proyecto.servicios.model.ProductosResponse;
import com.proyecto.servicios.model.ServicioCatalogo;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * Consulta el catalogo desde MongoDB; la carga desde GestoPago la hace CatalogoProductoService.
 * Cada producto incluye la URL de la imagen de su servicio, publicada por GestoPago.
 */
@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    public static final String URL_IMAGENES_DEFAULT = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/";
    private static final Sort ORDEN_CATALOGO = Sort.by("servicio", "nombre");

    private final CatProductoRepository catProductoRepository;
    private final GestoPagoProductoMapper productoMapper;
    private final String urlImagenes;

    public ProductoServiceImpl(CatProductoRepository catProductoRepository, GestoPagoProductoMapper productoMapper,
                               @Value("${gestopago.imagenes.url-base:" + URL_IMAGENES_DEFAULT + "}") String urlImagenes) {
        this.catProductoRepository = catProductoRepository;
        this.productoMapper = productoMapper;
        this.urlImagenes = urlImagenes.endsWith("/") ? urlImagenes : urlImagenes + "/";
    }

    @Override
    public ProductosResponse obtenerProductos() {
        ProductosResponse response = new ProductosResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setProductos(conImagen(consultarMongo(() -> catProductoRepository.findAll(ORDEN_CATALOGO))));
        return response;
    }

    @Override
    public CatalogoProductosResponse consultarProductos(ProductoFiltro filtro) {
        PageRequest pagina = PageRequest.of(filtro.getPagina(), filtro.getTamanio(), orden(filtro.getOrden()));
        Page<CatProducto> resultado = consultarMongo(() -> catProductoRepository.buscar(filtro, pagina));

        CatalogoProductosResponse response = new CatalogoProductosResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setProductos(conImagen(resultado.getContent()));
        response.asignarPaginacion(resultado);
        response.setServicios(consultarMongo(catProductoRepository::serviciosDistintos).stream()
                .map(p -> new ServicioCatalogo(p.getIdServicio(), p.getServicio(), imagen(p.getIdServicio())))
                .toList());
        response.setTiposServicio(consultarMongo(catProductoRepository::tiposServicio));
        return response;
    }

    static Sort orden(OrdenProducto orden) {
        return switch (orden) {
            case PRECIO_MENOR -> Sort.by(Sort.Order.asc("precio"), Sort.Order.asc("nombre"));
            case PRECIO_MAYOR -> Sort.by(Sort.Order.desc("precio"), Sort.Order.asc("nombre"));
            case NOMBRE -> ORDEN_CATALOGO;
        };
    }

    private List<ProductoResponse> conImagen(List<CatProducto> productos) {
        List<ProductoResponse> respuesta = productoMapper.toResponseList(productos);
        respuesta.forEach(p -> p.setImagenUrl(imagen(p.getIdServicio())));
        return respuesta;
    }

    /** GestoPago publica una imagen por servicio: {base}/{idServicio}.png */
    String imagen(Integer idServicio) {
        return idServicio == null ? null : urlImagenes + idServicio + ".png";
    }

    private <T> T consultarMongo(Supplier<T> consulta) {
        try {
            return consulta.get();
        } catch (DataAccessException e) {
            log.error("Fallo al consultar el catalogo en MongoDB: tipo={}", e.getClass().getSimpleName());
            throw new CatalogoProductosException("El catalogo de productos no esta disponible", e);
        }
    }
}
