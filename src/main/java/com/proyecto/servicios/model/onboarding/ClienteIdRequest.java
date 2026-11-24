package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identifica al cliente que se consulta en POST /clientes/detalle. */
@Getter
@Setter
@NoArgsConstructor
public class ClienteIdRequest {

    @Schema(description = "Id del cliente", example = "1")
    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer id;
}
