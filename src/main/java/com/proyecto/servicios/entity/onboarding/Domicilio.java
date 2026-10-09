package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "domicilios", schema = "onboarding")
@Getter
@Setter
public class Domicilio {

    public static final String PAIS_MEXICO = "MEX";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(name = "calle", nullable = false, length = 100)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 10)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 10)
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, length = 100)
    private String colonia;

    @Column(name = "municipio", nullable = false, length = 100)
    private String municipio;

    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "codigo_postal", nullable = false, length = 5)
    private String codigoPostal;

    @Column(name = "pais", nullable = false, length = 3)
    private String pais = PAIS_MEXICO;

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
