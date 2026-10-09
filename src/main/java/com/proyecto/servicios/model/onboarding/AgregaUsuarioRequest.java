package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Crea el usuario de acceso de un cliente que no tiene uno. El nombre de usuario es el correo del
 * cliente; la contrasena se valida con PoliticaContrasena y se guarda cifrada con BCrypt.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class AgregaUsuarioRequest {

    @Schema(example = "1")
    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;

    @Schema(example = "Segura#2026")
    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
