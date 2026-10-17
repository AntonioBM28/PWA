package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Actualizacion parcial de una cuenta: lo unico modificable es el estatus. El saldo solo cambia
 * con operaciones financieras. numeroCuenta se recibe solo para identificar la cuenta y no se
 * modifica; clienteId no se puede proporcionar como cambio.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaCuentaRequest {

    @Schema(description = "ACTIVA solo se permite si el cliente esta activo")
    @NotNull(message = "El estatus es obligatorio")
    private EstatusCuenta estatus;

    @Schema(description = "Identificador de la cuenta que se va a actualizar", example = "1000000001")
    @Null(message = "El saldo no se puede modificar directamente")
    private BigDecimal saldo;

    @Schema(hidden = true)
    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El numero de cuenta debe contener 10 digitos")
    private String numeroCuenta;

    @Schema(hidden = true)
    @Null(message = "El cliente de la cuenta no se puede modificar")
    private Integer clienteId;
}
