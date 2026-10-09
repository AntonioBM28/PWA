package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Actualizacion parcial de una cuenta: lo unico modificable es el estatus. El saldo solo cambia
 * con operaciones financieras, y el numero de cuenta y el cliente no se pueden cambiar; si se
 * envian, la peticion se rechaza.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaCuentaRequest {

    @Schema(description = "ACTIVA solo se permite si el cliente esta activo")
    @NotNull(message = "El estatus es obligatorio")
    private EstatusCuenta estatus;

    @Schema(hidden = true)
    @Null(message = "El saldo no se puede modificar directamente")
    private BigDecimal saldo;

    @Schema(hidden = true)
    @Null(message = "El numero de cuenta no se puede modificar")
    private String numeroCuenta;

    @Schema(hidden = true)
    @Null(message = "El cliente de la cuenta no se puede modificar")
    private Integer clienteId;
}
