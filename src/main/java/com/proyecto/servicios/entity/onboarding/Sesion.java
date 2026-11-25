package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.MotivoCierreSesion;
import com.proyecto.servicios.enums.Plataforma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sesion respaldada por el servidor. Su id viaja en el JWT (claim jti): una peticion solo se
 * acepta mientras la sesion siga abierta y vigente.
 */
@Entity
@Table(name = "sesiones", schema = "onboarding")
@Getter
@Setter
public class Sesion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 5, updatable = false)
    private Plataforma plataforma;

    /** Identificador de la instalacion que envia la app; nulo si el cliente no lo envia. */
    @Column(name = "dispositivo", length = 64, updatable = false)
    private String dispositivo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false, updatable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cierre", length = 12)
    private MotivoCierreSesion motivoCierre;

    public boolean estaAbierta(OffsetDateTime ahora) {
        return fechaCierre == null && fechaExpiracion.isAfter(ahora);
    }

    public void cerrar(MotivoCierreSesion motivo, OffsetDateTime ahora) {
        this.fechaCierre = ahora;
        this.motivoCierre = motivo;
    }
}
