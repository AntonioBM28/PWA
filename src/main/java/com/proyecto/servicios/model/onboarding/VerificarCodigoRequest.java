package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "codigo")
public class VerificarCodigoRequest {

    @Schema(description = "desafioId devuelto por POST /auth/login")
    @NotNull(message = "El identificador del desafio es obligatorio")
    private UUID desafioId;

    @Schema(description = "Codigo de 6 digitos recibido por correo", example = "123456")
    @NotBlank(message = "El codigo de verificacion es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El codigo de verificacion debe tener 6 digitos")
    private String codigo;
}
