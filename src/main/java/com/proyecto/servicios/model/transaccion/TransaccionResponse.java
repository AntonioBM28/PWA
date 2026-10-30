package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.enums.EstadoTransaccion;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class TransaccionResponse {
    UUID id;
    EstadoTransaccion estado;
    BigDecimal monto;
    String codigoProveedor;
    String idTx;
    String numeroAutorizacion;
    String pin;
    String folio;
    String leyenda;
    OffsetDateTime fechaCreacion;
    String mensaje;
}
