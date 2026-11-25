package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.Plataforma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Segundo factor de un inicio de sesion: se crea despues de validar la contrasena y se envia por
 * correo. Solo se guarda el hash del codigo. El id es el "desafio" que la app envia junto con el
 * codigo; no permite iniciar sesion por si solo.
 */
@Entity
@Table(name = "codigos_verificacion", schema = "onboarding")
@Getter
@Setter
public class CodigoVerificacion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Integer usuarioId;

    /** Hash BCrypt del codigo de 6 digitos. Cambia en cada reenvio. */
    @Column(name = "codigo_hash", nullable = false, length = 60)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "plataforma", nullable = false, length = 5, updatable = false)
    private Plataforma plataforma;

    @Column(name = "dispositivo", length = 64, updatable = false)
    private String dispositivo;

    /** El usuario ya acepto cerrar la sesion que tiene abierta en otro dispositivo. */
    @Column(name = "cerrar_sesion_anterior", nullable = false, updatable = false)
    private boolean cerrarSesionAnterior;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "reenvios", nullable = false)
    private short reenvios;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_envio", nullable = false)
    private OffsetDateTime fechaEnvio;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime fechaExpiracion;

    @Column(name = "fecha_uso")
    private OffsetDateTime fechaUso;

    public boolean estaVigente(OffsetDateTime ahora) {
        return fechaUso == null && fechaExpiracion.isAfter(ahora);
    }
}
