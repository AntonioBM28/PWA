package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Codigo postal a consultar en el servicio externo. */
@Getter
@Setter
@NoArgsConstructor
public class ConsultaCodigoPostalRequest {

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El codigo postal debe contener exactamente 5 digitos")
    private String codigoPostal;
}
