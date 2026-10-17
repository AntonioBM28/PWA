package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identificador de cliente para consultar su detalle. */
@Getter
@Setter
@NoArgsConstructor
public class ConsultaClienteRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;
}
