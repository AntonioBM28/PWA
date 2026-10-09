package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apertura de una cuenta adicional. El numero de cuenta, el saldo inicial y el estatus (ACTIVA)
 * los define el sistema.
 */
@Getter
@Setter
@NoArgsConstructor
public class CrearCuentaRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;
}
