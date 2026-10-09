package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ClienteResponse {
    private Integer id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private DomicilioResponse domicilio;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
