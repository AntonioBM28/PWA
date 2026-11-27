package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** NIP con el que el cliente autoriza sus compras. Solo se guarda el hash BCrypt. */
@Entity
@Table(name = "nips_transaccion", schema = "onboarding")
@Getter
@Setter
public class NipTransaccion {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "nip_hash", nullable = false, length = 60)
    private String nipHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
