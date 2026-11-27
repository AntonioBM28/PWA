package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Alta (POST /nip) o cambio (PATCH /nip) del NIP de transacciones. nipActual solo se usa al
 * cambiarlo. Las reglas (no repetidos ni consecutivos) las valida NipService.
 */
@Getter
@Setter
@ToString(exclude = {"nipActual", "nip", "confirmacion"})
public class NipRequest {

    @Pattern(regexp = "^[0-9]{4}$", message = "El NIP actual debe tener 4 digitos")
    private String nipActual;

    @NotBlank(message = "El NIP es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "El NIP debe tener exactamente 4 digitos")
    private String nip;

    @NotBlank(message = "Confirma tu NIP")
    private String confirmacion;
}
