package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios", schema = "onboarding")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Obligatorio para el rol CLIENTE; un EJECUTIVO no es cliente y no lo tiene. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", updatable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 10, updatable = false)
    private Rol rol = Rol.CLIENTE;

    /**
     * En un CLIENTE siempre es el correo del cliente: la FK (cliente_id, correo) lo actualiza en
     * cascada desde clientes, por eso la aplicacion no lo modifica.
     */
    @Column(name = "correo", nullable = false, length = 100, updatable = false)
    private String correo;

    /** Hash BCrypt, nunca la contrasena en texto plano. */
    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

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
