package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Numero de cuenta para consultar su detalle o saldo. */
@Getter
@Setter
@NoArgsConstructor
public class CuentaNumeroRequest {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El numero de cuenta debe contener 10 digitos")
    private String numeroCuenta;
}
