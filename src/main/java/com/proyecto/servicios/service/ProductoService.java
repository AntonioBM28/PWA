package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CatalogoProductosResponse;
import com.proyecto.servicios.model.ProductoFiltro;
import com.proyecto.servicios.model.ProductosResponse;

public interface ProductoService {
    ProductosResponse obtenerProductos();

    /** Consulta paginada del catalogo con filtros combinables (solo lectura). */
    CatalogoProductosResponse consultarProductos(ProductoFiltro filtro);
}
