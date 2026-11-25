package com.proyecto.servicios.model;

import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Codigo postal que se consulta en POST /codigos-postales/consulta. */
@Getter
@Setter
@NoArgsConstructor
public class CodigoPostalRequest {

    @Schema(description = "Codigo postal mexicano de 5 digitos", example = "44100")
    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = Patrones.CODIGO_POSTAL, message = Patrones.MENSAJE_CODIGO_POSTAL)
    private String codigoPostal;
}
