package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.Patrones;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Actualizacion parcial del domicilio. Si cambia el codigo postal o la colonia, el domicilio se
 * vuelve a validar con la API de codigos postales (y se actualizan estado y municipio); al cambiar
 * el codigo postal normalmente tambien hay que enviar la colonia que le corresponde.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaDomicilioRequest {

    @Size(max = 100, message = "La calle admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.NO_VACIO, message = "La calle no puede estar vacia")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La calle " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String calle;

    @Size(max = 10, message = "El numero exterior admite maximo 10 caracteres")
    @Pattern(regexp = Patrones.NO_VACIO, message = "El numero exterior no puede estar vacio")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El numero exterior " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String numeroExterior;

    /** Vacio ("") elimina el numero interior. */
    @Size(max = 10, message = "El numero interior admite maximo 10 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El numero interior " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String numeroInterior;

    @Size(max = 100, message = "La colonia admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.NO_VACIO, message = "La colonia no puede estar vacia")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La colonia " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String colonia;

    @Pattern(regexp = Patrones.CODIGO_POSTAL, message = Patrones.MENSAJE_CODIGO_POSTAL)
    private String codigoPostal;

    public boolean sinCambios() {
        return ObjectUtils.allNull(calle, numeroExterior, numeroInterior, colonia, codigoPostal);
    }

    /** true si cambia algun dato que se valida contra la API de codigos postales. */
    public boolean cambiaUbicacion() {
        return colonia != null || codigoPostal != null;
    }
}
