package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AbonoSaldoResponse extends GenericResponse {
    private UUID abonoId;
    private String numeroCuenta;
    private BigDecimal monto;
    private BigDecimal saldoAnterior;
    private BigDecimal saldo;
    private OffsetDateTime fecha;
}
