package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resultado de una transaccion. Hereda codigo y mensaje de GenericResponse, igual que el resto
 * de las respuestas de la API: codigo 0 si la operacion fue aceptada y 1 si fue rechazada.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransaccionResponse extends GenericResponse {
    private UUID id;
    private EstadoTransaccion estado;
    private BigDecimal monto;
    private String codigoProveedor;
    private String idTx;
    private String numeroAutorizacion;
    private String pin;
    private String folio;
    private String leyenda;
    private OffsetDateTime fechaCreacion;
}
