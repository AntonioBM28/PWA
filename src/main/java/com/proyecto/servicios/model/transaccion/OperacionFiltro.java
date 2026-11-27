package com.proyecto.servicios.model.transaccion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.onboarding.FiltroPaginado;
import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filtros de POST /transacciones/historial (cuerpo JSON). Todos son opcionales y se combinan (AND).
 * Un CLIENTE solo ve sus propias operaciones: clienteId y nombreCliente solo los usa el EJECUTIVO.
 */
@Getter
@Setter
@NoArgsConstructor
public class OperacionFiltro extends FiltroPaginado {

    @Schema(description = "Cliente exacto (solo EJECUTIVO)", example = "7")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;

    @Schema(description = "Texto contenido en el nombre o apellidos del cliente (solo EJECUTIVO)", example = "balderas")
    @Size(max = 50, message = "El nombre del cliente admite maximo 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]*$", message = "El nombre del cliente solo admite letras y espacios")
    private String nombreCliente;

    @Schema(description = "Numero de cuenta que pago la operacion", example = "1000000009")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = Patrones.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "Estado de la operacion")
    private EstadoTransaccion estado;

    @Schema(description = "Texto contenido en el producto o el servicio", example = "telcel")
    @Size(max = 60, message = "El texto de busqueda admite maximo 60 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El texto de busqueda " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String texto;

    @Schema(description = "Desde esta fecha (inclusive), formato AAAA-MM-DD", example = "2026-11-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesde;

    @Schema(description = "Hasta esta fecha (inclusive), formato AAAA-MM-DD", example = "2026-11-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaHasta;

    @AssertTrue(message = "La fecha inicial no puede ser posterior a la final")
    @JsonIgnore
    public boolean isRangoFechasValido() {
        return fechaDesde == null || fechaHasta == null || !fechaDesde.isAfter(fechaHasta);
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = StringUtils.trimToNull(nombreCliente);
    }

    public void setTexto(String texto) {
        this.texto = StringUtils.trimToNull(texto);
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = StringUtils.trimToNull(numeroCuenta);
    }
}
