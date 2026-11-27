package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.validation.Patrones;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos de una compra: se envian igual al pedir el token dinamico y al ejecutar la compra; el
 * token queda ligado a estos datos (si cambia alguno, el token ya no sirve).
 */
@Getter
@Setter
public abstract class DatosCompra {

    /** Cuenta que paga la operacion; un CLIENTE solo puede usar una cuenta propia. */
    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = Patrones.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @NotNull(message = "El servicio es obligatorio")
    private Integer idServicio;

    @NotNull(message = "El producto es obligatorio")
    private Integer idProducto;

    /** Teléfono o referencia según el tipo de producto del catálogo. */
    @Size(max = 100, message = "El destino admite maximo 100 caracteres")
    private String destino;

    /** Requerida para recibos/servicios; el catálogo decide si se envía. */
    @Size(max = 100, message = "La referencia admite maximo 100 caracteres")
    private String referencia;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
    private BigDecimal monto;
}
