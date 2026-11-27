package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.enums.OrdenProducto;
import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.CatalogoProductosResponse;
import com.proyecto.servicios.model.ProductoFiltro;
import com.proyecto.servicios.model.ServicioCatalogo;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaCatalogoTest {

    @Mock
    private CatProductoRepository catProductoRepository;

    private ProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductoServiceImpl(catProductoRepository, Mappers.getMapper(GestoPagoProductoMapper.class),
                "https://img.test/servicios");
    }

    @Test
    void consultar_devuelvePaginaConImagenYOpcionesDeFiltro() {
        ProductoFiltro filtro = new ProductoFiltro();
        filtro.setTexto("telcel");
        filtro.setTamanio(10);
        filtro.setOrden(OrdenProducto.PRECIO_MENOR);
        when(catProductoRepository.buscar(eq(filtro), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(producto(200, 79, "Telcel $100", "100")), PageRequest.of(0, 10), 1));
        when(catProductoRepository.serviciosDistintos()).thenReturn(List.of(producto(200, 79, "Telcel $100", "100")));
        when(catProductoRepository.tiposServicio()).thenReturn(List.of(1, 10));

        CatalogoProductosResponse response = service.consultarProductos(filtro);

        assertEquals(1, response.getProductos().size());
        assertEquals("https://img.test/servicios/79.png", response.getProductos().get(0).getImagenUrl());
        assertEquals(1, response.getTotalElementos());
        assertEquals(List.of(new ServicioCatalogo(79, "Telcel", "https://img.test/servicios/79.png")),
                response.getServicios());
        assertEquals(List.of(1, 10), response.getTiposServicio());

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(catProductoRepository).buscar(eq(filtro), pagina.capture());
        assertEquals(10, pagina.getValue().getPageSize());
        assertEquals(Sort.Direction.ASC, pagina.getValue().getSort().getOrderFor("precio").getDirection());
    }

    @Test
    void consultar_mongoNoDisponible() {
        when(catProductoRepository.buscar(any(), any())).thenThrow(new DataAccessResourceFailureException("caido"));

        assertThrows(CatalogoProductosException.class, () -> service.consultarProductos(new ProductoFiltro()));
    }

    @Test
    void orden_porPrecioMayor() {
        assertEquals(Sort.Direction.DESC,
                ProductoServiceImpl.orden(OrdenProducto.PRECIO_MAYOR).getOrderFor("precio").getDirection());
        assertEquals(Sort.Direction.ASC,
                ProductoServiceImpl.orden(OrdenProducto.NOMBRE).getOrderFor("servicio").getDirection());
    }

    @Test
    void filtro_validaciones() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            ProductoFiltro filtro = new ProductoFiltro();
            filtro.setTexto("  ");
            assertNull(filtro.getTexto());
            filtro.setOrden(null);
            assertEquals(OrdenProducto.NOMBRE, filtro.getOrden());
            assertTrue(validator.validate(filtro).isEmpty());

            filtro.setPrecioMin(new BigDecimal("500"));
            filtro.setPrecioMax(new BigDecimal("100"));
            filtro.setTexto("<script>");
            filtro.setIdServicio(-1);
            Set<String> mensajes = validator.validate(filtro).stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            assertTrue(mensajes.contains("El precio minimo no puede ser mayor que el precio maximo"), mensajes::toString);
            assertTrue(mensajes.contains("El servicio debe ser un numero positivo"), mensajes::toString);
            assertTrue(mensajes.stream().anyMatch(m -> m.startsWith("El texto de busqueda")), mensajes::toString);
        }
    }

    private static CatProducto producto(int id, int idServicio, String nombre, String precio) {
        CatProducto producto = new CatProducto();
        producto.setIdProducto(id);
        producto.setIdServicio(idServicio);
        producto.setIdCatTipoServicio(10);
        producto.setNombre(nombre);
        producto.setServicio("Telcel");
        producto.setPrecio(new BigDecimal(precio));
        return producto;
    }
}
