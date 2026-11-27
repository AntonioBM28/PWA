package com.proyecto.servicios.model.transaccion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstadoTransaccion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Una operacion (compra o pago de servicio) en el historial. */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperacionResponse {
    private UUID id;
    private OffsetDateTime fecha;
    private EstadoTransaccion estado;
    private BigDecimal monto;
    private Integer idProducto;
    private Integer idServicio;
    private String producto;
    private String servicio;
    private String imagenUrl;
    /** Telefono o referencia enmascarada. */
    private String referencia;
    private String numeroCuenta;
    private String numeroAutorizacion;
    private String folio;
    /** Mensaje del resultado (aprobada, rechazada por GestoPago, pendiente de confirmar...). */
    private String detalle;
    /** Motivo del resultado registrado (por ejemplo, por que se rechazo). */
    private String motivo;
    private String codigoProveedor;
    private String upc;
    private Integer clienteId;
    /** Solo en la consulta del EJECUTIVO. */
    private String nombreCliente;
}
