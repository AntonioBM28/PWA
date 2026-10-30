package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CrearTransaccionRequest {

    @NotNull
    private Integer idServicio;

    @NotNull
    private Integer idProducto;

    /** Teléfono o referencia según el tipo de producto del catálogo. */
    @Size(max = 100)
    private String destino;

    /** Requerida para recibos/servicios; el catálogo decide si se envía. */
    @Size(max = 100)
    private String referencia;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal monto;
}
