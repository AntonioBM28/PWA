package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.ProductoResponse;
import com.proyecto.servicios.model.ProductosResponse;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private CatProductoRepository catProductoRepository;

    private final GestoPagoProductoMapper productoMapper = Mappers.getMapper(GestoPagoProductoMapper.class);

    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(catProductoRepository, productoMapper);
    }

    @Test
    void obtenerProductos_catalogoEnMongo_devuelveProductos() {
        when(catProductoRepository.findAll(any(Sort.class))).thenReturn(List.of(producto()));

        ProductosResponse response = service.obtenerProductos();

        assertEquals(ProductoServiceImpl.CODIGO_EXITO, response.getCodigo());
        assertEquals(ProductoServiceImpl.MENSAJE_EXITO, response.getMensaje());
        assertEquals(1, response.getProductos().size());
        ProductoResponse producto = response.getProductos().get(0);
        assertEquals(185, producto.getIdProducto());
        assertEquals("Agua Cancun", producto.getNombre());
        assertEquals(new BigDecimal("10.0"), producto.getPrecio());
        assertEquals("Leyenda de prueba", producto.getLeyenda());
    }

    @Test
    void obtenerProductos_catalogoVacio_devuelveListaVacia() {
        when(catProductoRepository.findAll(any(Sort.class))).thenReturn(List.of());

        ProductosResponse response = service.obtenerProductos();

        assertEquals(ProductoServiceImpl.CODIGO_EXITO, response.getCodigo());
        assertTrue(response.getProductos().isEmpty());
    }

    @Test
    void obtenerProductos_mongoNoDisponible_lanzaErrorCatalogo() {
        when(catProductoRepository.findAll(any(Sort.class)))
                .thenThrow(new DataAccessResourceFailureException("Mongo caido"));

        assertThrows(CatalogoProductosException.class, () -> service.obtenerProductos());
    }

    private static CatProducto producto() {
        CatProducto producto = new CatProducto();
        producto.setIdProducto(185);
        producto.setIdServicio(56);
        producto.setNombre("Agua Cancun");
        producto.setServicio("AGUAKAN (Cancun)");
        producto.setPrecio(new BigDecimal("10.0"));
        producto.setLeyenda("Leyenda de prueba");
        return producto;
    }
}
