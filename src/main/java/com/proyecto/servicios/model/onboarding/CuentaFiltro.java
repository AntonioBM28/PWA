package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtros de GET /cuentas. Son opcionales y se combinan entre si (AND); la paginacion viene de
 * FiltroPaginado.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaFiltro extends FiltroPaginado {

    @Schema(description = "Cuentas del cliente indicado", example = "1")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;

    @Schema(description = "Cuentas con el estatus indicado")
    private EstatusCuenta estatus;
}
