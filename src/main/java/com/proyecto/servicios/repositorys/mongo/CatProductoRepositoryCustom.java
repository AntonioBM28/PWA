package com.proyecto.servicios.repositorys.mongo;

import com.proyecto.servicios.entity.mongo.CatProducto;

import java.util.List;

public interface CatProductoRepositoryCustom {

    /**
     * Inserta o actualiza los productos en una sola operacion bulk y elimina los que ya no vienen
     * en el catalogo. La coleccion nunca queda vacia durante la actualizacion.
     */
    void reemplazarCatalogo(List<CatProducto> productos);
}
