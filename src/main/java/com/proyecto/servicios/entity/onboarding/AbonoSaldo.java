package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Registro de auditoria de cada abono de saldo hecho por un ejecutivo. */
@Entity
@Table(name = "abonos_saldo", schema = "onboarding")
@Getter
@Setter
public class AbonoSaldo {

    @Id
    private UUID id;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private Integer cuentaId;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal monto;

    @Column(name = "saldo_anterior", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_nuevo", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal saldoNuevo;

    @Column(name = "concepto", length = 100, updatable = false)
    private String concepto;

    @Column(name = "ejecutivo", nullable = false, length = 100, updatable = false)
    private String ejecutivo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
