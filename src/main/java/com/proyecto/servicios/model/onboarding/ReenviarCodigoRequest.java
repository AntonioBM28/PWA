package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReenviarCodigoRequest {

    @Schema(description = "desafioId devuelto por POST /auth/login")
    @NotNull(message = "El identificador del desafio es obligatorio")
    private UUID desafioId;
}
