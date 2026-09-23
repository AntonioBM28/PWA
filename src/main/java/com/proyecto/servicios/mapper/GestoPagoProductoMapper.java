package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.model.ProductoResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface GestoPagoProductoMapper {

    @Mapping(target = "nombre", source = "producto.producto")
    @Mapping(target = "leyenda", source = "producto.legend")
    CatProducto toDocument(GestoPagoProducto producto, Instant fechaActualizacion);

    ProductoResponse toResponse(CatProducto producto);

    List<ProductoResponse> toResponseList(List<CatProducto> productos);
}
