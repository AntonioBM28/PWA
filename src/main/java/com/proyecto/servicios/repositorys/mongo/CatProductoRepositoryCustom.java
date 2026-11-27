package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.model.ProductoFiltro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CatProductoRepositoryCustom {

    /**
     * Inserta o actualiza los productos en una sola operacion bulk y elimina los que ya no vienen
     * en el catalogo. La coleccion nunca queda vacia durante la actualizacion.
     */
    void reemplazarCatalogo(List<CatProducto> productos);

    /** Busqueda con filtros combinables (AND); el orden viene en el Pageable. */
    Page<CatProducto> buscar(ProductoFiltro filtro, Pageable pagina);

    /** Un producto por cada servicio (proveedor) del catalogo, ordenados por nombre del servicio. */
    List<CatProducto> serviciosDistintos();

    /** Tipos de servicio existentes en el catalogo. */
    List<Integer> tiposServicio();
}
