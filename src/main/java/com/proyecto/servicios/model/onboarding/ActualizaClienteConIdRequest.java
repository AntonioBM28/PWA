package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identificador y cambios parciales del cliente que se va a actualizar. */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaClienteConIdRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;

    @NotNull(message = "Los datos de actualizacion son obligatorios")
    @Valid
    private ActualizaClienteRequest cambios;
}
