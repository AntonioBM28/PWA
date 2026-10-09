package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Filtros de GET /clientes. Todos son opcionales y se combinan entre si (AND); la paginacion
 * viene de FiltroPaginado.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteFiltro extends FiltroPaginado {

    @Schema(description = "Nombre o inicio del nombre, sin distinguir mayusculas", example = "Mar")
    @Size(max = 50, message = "El filtro nombre admite maximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El filtro nombre " + Patrones.MENSAJE_NOMBRE)
    private String nombre;

    @Schema(description = "Apellido paterno o su inicio, sin distinguir mayusculas", example = "Garcia")
    @Size(max = 50, message = "El filtro apellidoPaterno admite maximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El filtro apellidoPaterno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoPaterno;

    @Schema(description = "Apellido materno o su inicio, sin distinguir mayusculas", example = "Lopez")
    @Size(max = 50, message = "El filtro apellidoMaterno admite maximo 50 caracteres")
    @Pattern(regexp = Patrones.NOMBRE, message = "El filtro apellidoMaterno " + Patrones.MENSAJE_NOMBRE)
    private String apellidoMaterno;

    @Schema(description = "CURP exacta", example = "GALM850320MJCRPR05")
    @Pattern(regexp = Patrones.CURP, message = Patrones.MENSAJE_CURP)
    private String curp;

    @Schema(description = "RFC exacto", example = "GALM850320KX3")
    @Pattern(regexp = Patrones.RFC, message = Patrones.MENSAJE_RFC)
    private String rfc;

    @Schema(description = "Correo electronico exacto", example = "maria.garcia@mail.com")
    @Pattern(regexp = Patrones.CORREO, message = Patrones.MENSAJE_CORREO)
    private String correo;

    @Schema(description = "Numero de cuenta del cliente", example = "1000000000")
    @Pattern(regexp = "^[0-9]{10}$", message = "El numero de cuenta debe contener 10 digitos")
    private String numeroCuenta;

    @Schema(description = "true: solo clientes activos; false: solo dados de baja")
    private Boolean activo;

    @Schema(description = "Registrados desde esta fecha (inclusive), formato AAAA-MM-DD", example = "2026-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRegistroDesde;

    @Schema(description = "Registrados hasta esta fecha (inclusive), formato AAAA-MM-DD", example = "2026-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRegistroHasta;

    public void setNombre(String nombre) {
        this.nombre = StringUtils.trimToNull(nombre);
    }

    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = StringUtils.trimToNull(apellidoPaterno);
    }

    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = StringUtils.trimToNull(apellidoMaterno);
    }

    public void setCurp(String curp) {
        this.curp = StringUtils.upperCase(StringUtils.trimToNull(curp));
    }

    public void setRfc(String rfc) {
        this.rfc = StringUtils.upperCase(StringUtils.trimToNull(rfc));
    }

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trimToNull(correo));
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = StringUtils.trimToNull(numeroCuenta);
    }
}
