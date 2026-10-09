package com.proyecto.servicios.entity.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Las cuentas y el usuario no se mapean desde aqui para no cargarlos en cada consulta de
 * clientes; se obtienen con CuentaRepository y UsuarioRepository.
 */
@Entity
@Table(name = "clientes", schema = "onboarding")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 50)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", nullable = false, length = 18, updatable = false)
    private String curp;

    @Column(name = "rfc", nullable = false, length = 13, updatable = false)
    private String rfc;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sexo", nullable = false, length = 1)
    private Sexo sexo;

    @Column(name = "nacionalidad", nullable = false, length = 3)
    private String nacionalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_civil", nullable = false, length = 12)
    private EstadoCivil estadoCivil;

    @Column(name = "correo", nullable = false, length = 100)
    private String correo;

    @Column(name = "telefono_movil", nullable = false, length = 10)
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", length = 10)
    private String telefonoAlternativo;

    @Column(name = "ocupacion", nullable = false, length = 60)
    private String ocupacion;

    @Column(name = "empresa", nullable = false, length = 100)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, optional = false)
    private Domicilio domicilio;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public void asignarDomicilio(Domicilio domicilio) {
        domicilio.setCliente(this);
        this.domicilio = domicilio;
    }

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
