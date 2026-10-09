package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;
import com.proyecto.servicios.validation.MayorDeEdad;
import com.proyecto.servicios.validation.Patrones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para registrar un cliente. CURP, RFC y nacionalidad se convierten a mayusculas y el
 * correo a minusculas al recibirlos, para que la validacion y la unicidad no dependan de como
 * se capturaron.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    // ----- Datos personales -----
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El nombre " + Patrones.MENSAJE_NOMBRE)
    private String nombre;

    @Size(min = 2, max = 50, message = "El segundo nombre " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El segundo nombre " + Patrones.MENSAJE_NOMBRE)
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido paterno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno " + Patrones.MENSAJE_LONGITUD_NOMBRE)
    @Pattern(regexp = Patrones.NOMBRE, message = "El apellido materno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @MayorDeEdad
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Size(min = 18, max = 18, message = Patrones.MENSAJE_LONGITUD_CURP)
    @Pattern(regexp = Patrones.CURP, message = Patrones.MENSAJE_CURP)
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 13, max = 13, message = Patrones.MENSAJE_LONGITUD_RFC)
    @Pattern(regexp = Patrones.RFC, message = Patrones.MENSAJE_RFC)
    private String rfc;

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Pattern(regexp = Patrones.NACIONALIDAD, message = Patrones.MENSAJE_NACIONALIDAD)
    private String nacionalidad;

    @NotNull(message = "El estado civil es obligatorio")
    private EstadoCivil estadoCivil;

    // ----- Datos de contacto -----
    @NotBlank(message = "El correo electronico es obligatorio")
    @Size(max = 100, message = Patrones.MENSAJE_LONGITUD_CORREO)
    @Pattern(regexp = Patrones.CORREO, message = Patrones.MENSAJE_CORREO)
    private String correo;

    @NotBlank(message = "El telefono movil es obligatorio")
    @Pattern(regexp = Patrones.TELEFONO, message = "El telefono movil " + Patrones.MENSAJE_TELEFONO)
    private String telefonoMovil;

    @Pattern(regexp = Patrones.TELEFONO, message = "El telefono alternativo " + Patrones.MENSAJE_TELEFONO)
    private String telefonoAlternativo;

    // ----- Domicilio -----
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // ----- Informacion laboral -----
    @NotBlank(message = "La ocupacion es obligatoria")
    @Size(max = 60, message = "La ocupacion admite maximo 60 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La ocupacion " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "La empresa admite maximo 100 caracteres")
    @Pattern(regexp = Patrones.TEXTO_SEGURO, message = "La empresa " + Patrones.MENSAJE_TEXTO_SEGURO)
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual admite hasta 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    // ----- Acceso -----
    /** Las reglas de complejidad las valida PoliticaContrasena para dar un mensaje por cada regla. */
    @NotBlank(message = "La contrasena es obligatoria")
    private String password;

    public void setCurp(String curp) {
        this.curp = StringUtils.upperCase(StringUtils.trim(curp));
    }

    public void setRfc(String rfc) {
        this.rfc = StringUtils.upperCase(StringUtils.trim(rfc));
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = StringUtils.upperCase(StringUtils.trim(nacionalidad));
    }

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trim(correo));
    }
}
