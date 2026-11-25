package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.Plataforma;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/** Primer paso del inicio de sesion: correo y contrasena. */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class LoginRequest {

    @Schema(description = "Correo electronico con el que se registro el usuario", example = "maria.garcia@mail.com")
    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 100, message = "El correo electronico admite maximo 100 caracteres")
    private String correo;

    @Schema(example = "Maria#2026")
    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 72, message = "La contrasena admite maximo 72 caracteres")
    private String password;

    @Schema(description = "WEB: la sesion viaja en una cookie HttpOnly. MOVIL (por defecto): el token se "
            + "devuelve en la respuesta para enviarlo como Authorization: Bearer")
    private Plataforma plataforma = Plataforma.MOVIL;

    @Schema(description = "Identificador de la instalacion de la app (UUID); permite reconocer el mismo dispositivo",
            example = "3f6c2a1e-8b5d-4f0a-9c7e-1d2b3a4c5d6e")
    @Size(max = 64, message = "El identificador del dispositivo admite maximo 64 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "El identificador del dispositivo solo admite letras, numeros y guiones")
    private String dispositivo;

    @Schema(description = "true si el usuario acepto cerrar la sesion que tiene abierta en otro dispositivo")
    private boolean cerrarSesionAnterior;

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trim(correo));
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma == null ? Plataforma.MOVIL : plataforma;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = StringUtils.trimToNull(dispositivo);
    }
}
