package com.proyecto.servicios.model.transaccion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/** Ejecuta una compra autorizada con el token dinamico de POST /transacciones/autorizacion. */
@Getter
@Setter
@ToString(exclude = "tokenDinamico")
public class CrearTransaccionRequest extends DatosCompra {

    @Schema(description = "autorizacionId devuelto por POST /transacciones/autorizacion")
    @NotNull(message = "La autorizacion es obligatoria; primero confirma la operacion con tu NIP")
    private UUID autorizacionId;

    @Schema(description = "Token dinamico de 8 digitos", example = "48213907")
    @NotBlank(message = "El token dinamico es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El token dinamico debe tener 8 digitos")
    private String tokenDinamico;
}
