package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Ocupacion;
import com.proyecto.servicios.enums.RangoIngresoMensual;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.validation.MayorDeEdad;
import com.proyecto.servicios.validation.Patrones;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

/**
 * Actualizacion parcial de un cliente (PATCH /clientes): el campo id indica que cliente se
 * actualiza y no cuenta como cambio. Solo se modifican los campos enviados; un campo
 * nulo o ausente no cambia. En los campos opcionales (segundo nombre, telefono alternativo y
 * numero interior) un valor vacio ("") elimina el dato.
 * <p>
 * activo=false da de baja logica al cliente (sus cuentas y su usuario quedan inactivos);
 * activo=true lo reactiva junto con su usuario.
 * <p>
 * CURP, RFC y numero de cuenta no se pueden modificar: si se envian, la peticion se rechaza.
 */
@Getter
@Setter
@NoArgsConstructor
public class ActualizaClienteRequest {

    // ----- Cliente a actualizar (identifica al registro, no se modifica) -----
    @Schema(description = "Id del cliente a actualizar", example = "1")
    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer id;

    // ----- Datos personales -----
    @Size(min = 2, max = 50, message = "El nombre " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El nombre " + Patrones.MENSAJE_NOMBRE)
    private String nombre;

    @Pattern(regexp = Patrones.NOMBRE_O_VACIO, message = "El segundo nombre " + Patrones.MENSAJE_NOMBRE_O_VACIO)
    private String segundoNombre;

    @Size(min = 2, max = 50, message = "El apellido paterno " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido paterno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoPaterno;

    @Size(min = 2, max = 50, message = "El apellido materno " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido materno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoMaterno;

    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @MayorDeEdad
    private LocalDate fechaNacimiento;

    private Sexo sexo;

    @Pattern(regexp = Patrones.NACIONALIDAD, message = Patrones.MENSAJE_NACIONALIDAD)
    private String nacionalidad;

    private EstadoCivil estadoCivil;

    // ----- Datos de contacto -----
    @Size(max = 100, message = Patrones.MENSAJE_LONGITUD_CORREO)
    @Email(message = Patrones.MENSAJE_CORREO)
    @Pattern(regexp = Patrones.CORREO, message = Patrones.MENSAJE_CORREO)
    private String correo;

    @Pattern(regexp = Patrones.TELEFONO, message = "El telefono movil " + Patrones.MENSAJE_TELEFONO)
    private String telefonoMovil;

    @Pattern(regexp = Patrones.TELEFONO_O_VACIO,
            message = "El telefono alternativo " + Patrones.MENSAJE_TELEFONO + ", o enviarse vacio para eliminarlo")
    private String telefonoAlternativo;

    // ----- Domicilio -----
    @Valid
    private ActualizaDomicilioRequest domicilio;

    // ----- Informacion laboral (opcional: un valor vacio elimina el dato) -----
    @Schema(description = "Clave del catalogo de ocupaciones, o vacio para eliminarla", example = "DOCENTE")
    private String ocupacion;

    @Size(max = 100, message = "La empresa admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La empresa " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String empresa;

    @Schema(description = "Clave del tabulador de ingresos, o vacio para eliminarlo", example = "DE_10001_A_20000")
    private String rangoIngresoMensual;

    // ----- Estatus -----
    @Schema(description = "false: baja logica (cuentas y usuario quedan inactivos); true: reactiva al cliente y a su usuario")
    private Boolean activo;

    // ----- Datos que no se pueden modificar -----
    @Schema(hidden = true)
    @Null(message = "La CURP no se puede modificar")
    private String curp;

    @Schema(hidden = true)
    @Null(message = "El RFC no se puede modificar")
    private String rfc;

    @Schema(hidden = true)
    @Null(message = "El numero de cuenta no se puede modificar")
    private String numeroCuenta;

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = StringUtils.upperCase(StringUtils.trim(nacionalidad));
    }

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trim(correo));
    }

    @JsonIgnore
    @AssertTrue(message = "La ocupacion no existe en el catalogo")
    public boolean isOcupacionValida() {
        return StringUtils.isEmpty(ocupacion) || Ocupacion.existe(ocupacion);
    }

    @JsonIgnore
    @AssertTrue(message = "El rango de ingreso mensual no existe en el tabulador")
    public boolean isRangoIngresoMensualValido() {
        return StringUtils.isEmpty(rangoIngresoMensual) || RangoIngresoMensual.existe(rangoIngresoMensual);
    }

    /** true si la peticion no trae ningun dato para actualizar. */
    public boolean sinCambios() {
        return ObjectUtils.allNull(nombre, segundoNombre, apellidoPaterno, apellidoMaterno, fechaNacimiento, sexo,
                nacionalidad, estadoCivil, correo, telefonoMovil, telefonoAlternativo, ocupacion, empresa,
                rangoIngresoMensual, activo)
                && (domicilio == null || domicilio.sinCambios());
    }

    @AssertTrue(message = "No se envio ningun dato para actualizar")
    @JsonIgnore
    public boolean isSolicitudConCambios() {
        return !sinCambios();
    }
}
