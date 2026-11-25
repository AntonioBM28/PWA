package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identifica la cuenta que se consulta en POST /cuentas/detalle y POST /cuentas/saldo. */
@Getter
@Setter
@NoArgsConstructor
public class NumeroCuentaRequest {

    @Schema(description = "Numero de cuenta de 10 digitos", example = "1000000001")
    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = Patrones.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;
}
