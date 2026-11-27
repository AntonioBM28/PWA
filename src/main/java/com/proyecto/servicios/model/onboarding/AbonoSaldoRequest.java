package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/** Abono de saldo a una cuenta (solo EJECUTIVO): POST /cuentas/abonos. */
@Getter
@Setter
@NoArgsConstructor
public class AbonoSaldoRequest {

    /** Monto maximo de un solo abono (en pesos). */
    public static final String MONTO_MAXIMO = "50000.00";

    @Schema(description = "Numero de cuenta de 10 digitos", example = "1000000001")
    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Pattern(regexp = Patrones.NUMERO_CUENTA, message = Patrones.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "Monto a abonar en pesos, de 1.00 a 50,000.00", example = "1500.00")
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "1.00", message = "El monto minimo de un abono es de $1.00")
    @DecimalMax(value = MONTO_MAXIMO, message = "El monto maximo de un abono es de $50,000.00")
    @Digits(integer = 13, fraction = 2, message = "El monto admite maximo 2 decimales")
    private BigDecimal monto;

    @Schema(description = "Motivo del abono (opcional)", example = "Deposito en sucursal")
    @Size(max = 100, message = "El concepto admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "El concepto " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String concepto;

    public void setConcepto(String concepto) {
        this.concepto = StringUtils.trimToNull(concepto);
    }
}
