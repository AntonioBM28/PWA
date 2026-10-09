package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Un cliente con su domicilio y sus cuentas. */
@Getter
@Setter
@NoArgsConstructor
public class ClienteDetalleResponse extends GenericResponse {
    private ClienteResponse cliente;
    private List<CuentaResponse> cuentas;
}
