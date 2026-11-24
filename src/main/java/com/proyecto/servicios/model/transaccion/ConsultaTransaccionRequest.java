package com.proyecto.servicios.model.transaccion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Identifica la transaccion que se consulta en POST /transacciones/consulta. */
@Getter
@Setter
@NoArgsConstructor
public class ConsultaTransaccionRequest {

    @Schema(description = "Id de la transaccion devuelto al crearla")
    @NotNull(message = "El id de la transaccion es obligatorio")
    private UUID id;
}
