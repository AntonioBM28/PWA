package com.proyecto.servicios.service;

public interface CatalogoProductoService {

    /**
     * Descarga el catalogo de GestoPago y lo guarda en MongoDB.
     *
     * @return numero de productos sincronizados
     */
    int sincronizarCatalogo();
}
