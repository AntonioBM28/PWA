package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Actualizacion parcial de una cuenta (PATCH /cuentas): numeroCuenta indica que cuenta se
 * actualiza y lo unico modificable es el estatus. El saldo solo cambia con operaciones
 * financieras y el cliente no se puede cambiar; si se envian, la peticion se rechaza.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaCuentaRequest {

    @Schema(description = "Numero de la cuenta a actualizar (no se modifica)", example = "1000000001")
    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = Patrones.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "ACTIVA solo se permite si el cliente esta activo")
    @NotNull(message = "El estatus es obligatorio")
    private EstatusCuenta estatus;

    @Schema(hidden = true)
    @Null(message = "El saldo no se puede modificar directamente")
    private BigDecimal saldo;

    @Schema(hidden = true)
    @Null(message = "El cliente de la cuenta no se puede modificar")
    private Integer clienteId;
}
