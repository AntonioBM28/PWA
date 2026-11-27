package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.model.ProductoFiltro;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatProductoRepositoryImplTest {

    @Test
    void criterios_combinaSoloLosFiltrosConValor_yEscapaElTexto() {
        ProductoFiltro filtro = new ProductoFiltro();
        assertTrue(CatProductoRepositoryImpl.criterios(filtro).isEmpty());

        filtro.setTexto("a.b(");
        filtro.setIdServicio(79);
        filtro.setIdCatTipoServicio(10);
        filtro.setPrecioMin(new BigDecimal("50"));
        filtro.setPrecioMax(new BigDecimal("500"));
        var criterios = CatProductoRepositoryImpl.criterios(filtro);
        String consulta = new Query().addCriteria(criterios.get(0)).toString();

        assertEquals(4, criterios.size());
        // El texto se busca literal (\Q...\E), no como expresion regular
        assertTrue(consulta.contains("\\\\Qa.b(\\\\E"), consulta);
        assertTrue(consulta.contains("\"options\" : \"i\""), consulta);
    }

    @Test
    void buscar_cuentaYPaginaEnMongo() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.count(any(Query.class), eq(CatProducto.class))).thenReturn(25L);
        // Pagina 2 de 3 (llena): el total se obtiene con count
        List<CatProducto> diez = java.util.stream.IntStream.range(0, 10).mapToObj(i -> {
            CatProducto producto = new CatProducto();
            producto.setIdProducto(i);
            return producto;
        }).toList();
        when(mongo.find(any(Query.class), eq(CatProducto.class))).thenReturn(diez);

        var pagina = new CatProductoRepositoryImpl(mongo).buscar(new ProductoFiltro(), PageRequest.of(1, 10));

        assertEquals(25, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
    }
}
