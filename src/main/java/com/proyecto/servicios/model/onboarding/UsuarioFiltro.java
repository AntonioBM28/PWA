package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Filtros de GET /usuarios/filtro. Son opcionales y se combinan entre si (AND); la paginacion
 * viene de FiltroPaginado.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioFiltro extends FiltroPaginado {

    @Schema(description = "Correo o inicio del correo, sin distinguir mayusculas", example = "maria")
    @Size(max = 100, message = "El filtro correo admite maximo 100 caracteres")
    private String correo;

    @Schema(description = "true: solo usuarios activos; false: solo inactivos")
    private Boolean activo;

    @Schema(description = "Usuario del cliente indicado", example = "2")
    @Positive(message = "El id del cliente debe ser un numero positivo")
    private Integer clienteId;

    public void setCorreo(String correo) {
        this.correo = StringUtils.lowerCase(StringUtils.trimToNull(correo));
    }
}
