package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Token dinamico de un solo uso que autoriza una compra concreta. Solo se guarda su hash. */
@Entity
@Table(name = "autorizaciones_transaccion", schema = "onboarding")
@Getter
@Setter
public class AutorizacionTransaccion {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Column(name = "huella_solicitud", nullable = false, length = 64, updatable = false)
    private String huellaSolicitud;

    @Column(name = "token_hash", nullable = false, length = 60, updatable = false)
    private String tokenHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    /** Clave de idempotencia de la compra que lo uso: permite reintentar la misma compra. */
    @Column(name = "clave_idempotencia", length = 64)
    private String claveIdempotencia;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, updatable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;
}
