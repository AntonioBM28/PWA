package com.proyecto.servicios.entity.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto del catalogo GestoPago persistido en MongoDB. El nombre de la coleccion
 * se toma de la propiedad gestopago.productos.coleccion.
 */
@Document(collection = "#{@environment.getProperty('gestopago.productos.coleccion')}")
@Getter
@Setter
public class CatProducto {

    @Id
    private Integer idProducto;

    private Integer idServicio;

    private Integer idCatTipoServicio;

    private String nombre;

    private String servicio;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal precio;

    private Integer tipoFront;

    private Boolean hasDigitoVerificador;

    private Boolean showAyuda;

    private String tipoReferencia;

    private String leyenda;

    private Instant fechaActualizacion;
}
