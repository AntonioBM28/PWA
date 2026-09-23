package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductoResponse {
    private Integer idProducto;
    private Integer idServicio;
    private Integer idCatTipoServicio;
    private String nombre;
    private String servicio;
    private BigDecimal precio;
    private Integer tipoFront;
    private Boolean hasDigitoVerificador;
    private Boolean showAyuda;
    private String tipoReferencia;
    private String leyenda;
}
