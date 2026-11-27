package com.proyecto.servicios.model.transaccion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Confirma una compra con el NIP para obtener el token dinamico. */
@Getter
@Setter
@ToString(exclude = "nip")
public class AutorizacionTransaccionRequest extends DatosCompra {

    @Schema(description = "NIP de transacciones de 4 digitos", example = "2580")
    @NotBlank(message = "El NIP es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "El NIP debe tener 4 digitos")
    private String nip;
}
