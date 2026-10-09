package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {
    private String numeroCuenta;
    private Integer clienteId;
    private EstatusCuenta estatus;
    private BigDecimal saldo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
