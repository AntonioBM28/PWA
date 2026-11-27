package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.enums.OrdenProducto;
import com.proyecto.servicios.model.onboarding.FiltroPaginado;
import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * Filtros de POST /productos/consulta (cuerpo JSON). Todos son opcionales y se combinan entre si (AND);
 * la paginacion viene de FiltroPaginado.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoFiltro extends FiltroPaginado {

    @Schema(description = "Texto contenido en el nombre del producto o del servicio, sin distinguir mayusculas",
            example = "telcel")
    @Size(max = 60, message = "El texto de busqueda admite maximo 60 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El texto de busqueda " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String texto;

    @Schema(description = "Servicio (proveedor) exacto", example = "79")
    @Positive(message = "El servicio debe ser un numero positivo")
    private Integer idServicio;

    @Schema(description = "Tipo de servicio de GestoPago", example = "10")
    @Positive(message = "El tipo de servicio debe ser un numero positivo")
    private Integer idCatTipoServicio;

    @Schema(description = "Precio minimo (inclusive)", example = "50")
    @DecimalMin(value = "0", message = "El precio minimo no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio minimo admite maximo 2 decimales")
    private BigDecimal precioMin;

    @Schema(description = "Precio maximo (inclusive)", example = "500")
    @DecimalMin(value = "0", message = "El precio maximo no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio maximo admite maximo 2 decimales")
    private BigDecimal precioMax;

    @Schema(description = "Orden de los resultados", defaultValue = "NOMBRE")
    private OrdenProducto orden = OrdenProducto.NOMBRE;

    @AssertTrue(message = "El precio minimo no puede ser mayor que el precio maximo")
    @JsonIgnore
    public boolean isRangoPrecioValido() {
        return precioMin == null || precioMax == null || precioMin.compareTo(precioMax) <= 0;
    }

    public void setTexto(String texto) {
        this.texto = StringUtils.trimToNull(texto);
    }

    public void setOrden(OrdenProducto orden) {
        this.orden = orden == null ? OrdenProducto.NOMBRE : orden;
    }
}
