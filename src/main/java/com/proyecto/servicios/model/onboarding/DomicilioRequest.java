package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado, municipio y pais no se reciben: se obtienen del codigo postal con la API de codigos
 * postales, y la colonia debe ser una de las que esa API devuelve para el codigo postal.
 */
@Getter
@Setter
@NoArgsConstructor
public class DomicilioRequest {

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La calle " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String calle;

    @NotBlank(message = "El numero exterior es obligatorio")
    @Size(max = 10, message = "El numero exterior admite maximo 10 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El numero exterior " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String numeroExterior;

    @Size(max = 10, message = "El numero interior admite maximo 10 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El numero interior " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La colonia " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String colonia;

    @NotBlank(message = "El codigo postal es obligatorio")
    @Pattern(regexp = Patrones.CODIGO_POSTAL, message = Patrones.MENSAJE_CODIGO_POSTAL)
    private String codigoPostal;
}
