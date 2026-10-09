package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.Rol;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Datos de un usuario de acceso. La contrasena nunca se devuelve. */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioResponse {
    private Integer id;
    /** Nulo para un EJECUTIVO, que no es cliente. */
    private Integer clienteId;
    private String correo;
    private Rol rol;
    private Boolean activo;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
