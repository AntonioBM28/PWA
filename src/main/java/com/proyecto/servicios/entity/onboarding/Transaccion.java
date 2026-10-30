package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transacciones", schema = "onboarding",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_transacciones_cliente_idempotencia", columnNames = {"cliente_id", "clave_idempotencia"}),
                @UniqueConstraint(name = "uq_transacciones_upc", columnNames = "upc")
        })
@Getter
@Setter
public class Transaccion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Integer clienteId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private Integer cuentaId;

    @Column(name = "clave_idempotencia", nullable = false, length = 64, updatable = false)
    private String claveIdempotencia;

    @Column(name = "huella_solicitud", nullable = false, length = 64, updatable = false)
    private String huellaSolicitud;

    @Column(name = "upc", nullable = false, length = 15, updatable = false)
    private String upc;

    @Column(name = "payload_cifrado", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payloadCifrado;

    @Column(name = "random_proveedor", nullable = false, length = 32, updatable = false)
    private String randomProveedor;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(name = "codigo_proveedor", length = 8)
    private String codigoProveedor;

    @Column(name = "mensaje_proveedor", length = 500)
    private String mensajeProveedor;

    @Column(name = "id_tx", length = 32)
    private String idTx;

    @Column(name = "numero_autorizacion", length = 60)
    private String numeroAutorizacion;

    @Column(name = "pin", length = 250)
    private String pin;

    @Column(name = "folio", length = 250)
    private String folio;

    @Column(name = "leyenda", length = 5000)
    private String leyenda;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        fechaCreacion = OffsetDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
