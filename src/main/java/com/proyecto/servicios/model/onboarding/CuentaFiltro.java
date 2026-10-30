package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstatusCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtros de POST /cuentas/busqueda. Son opcionales y se combinan entre si (AND); la paginacion viene de
 * FiltroPaginado.
 */
@Getter
@Setter
@NoArgsConstructor
public class CuentaFiltro extends FiltroPaginado {

    @Schema(description = "Numero de cuenta exacto", example = "1000000000")
    @Pattern(regexp = "^[0-9]{10}$", message = "El numero de cuenta debe contener 10 digitos")
    private String numeroCuenta;

    @Schema(description = "Correo exacto del cliente propietario; CLIENTE debe indicar su propio correo", example = "maria.garcia@mail.com")
    @Email(message = "El correo del cliente debe tener un formato valido")
    @Size(max = 254, message = "El correo del cliente admite maximo 254 caracteres")
    private String correoCliente;

    @Schema(description = "Cuentas con el estatus indicado")
    private EstatusCuenta estatus;
}
