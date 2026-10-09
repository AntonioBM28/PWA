package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cuentas", schema = "onboarding")
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    /** Lo asigna la secuencia onboarding.seq_numero_cuenta al insertar; nunca se modifica. */
    @Generated
    @Column(name = "numero_cuenta", length = 10, insertable = false, updatable = false)
    private String numeroCuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 10)
    private EstatusCuenta estatus = EstatusCuenta.ACTIVA;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @PrePersist
    void onCreate() {
        fechaCreacion = OffsetDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void onUpdate() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
