package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class LoginRequest {

    @Schema(description = "Correo electronico con el que se registro el cliente", example = "maria.garcia@mail.com")
    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 100, message = "El correo electronico admite maximo 100 caracteres")
    private String correo;

    @Schema(example = "Maria#2026")
    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 72, message = "La contrasena admite maximo 72 caracteres")
    private String password;

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trim(correo));
    }
}
