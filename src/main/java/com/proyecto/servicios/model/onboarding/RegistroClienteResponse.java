package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado del registro: el cliente, la cuenta creada automaticamente y el correo con el que
 * su usuario de acceso inicia sesion. La contrasena nunca se devuelve.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistroClienteResponse extends GenericResponse {
    private ClienteResponse cliente;
    private CuentaResponse cuenta;
    private String usuario;
}
