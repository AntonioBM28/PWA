package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesion activa de quien hace la peticion (GET /auth/sesion). Nunca incluye el token. */
@Getter
@Setter
@NoArgsConstructor
public class SesionResponse extends GenericResponse {
    private String usuario;
    private Rol rol;
    /** Nulo para un EJECUTIVO, que no es cliente. */
    private Integer clienteId;
    private long expiraEnSegundos;
}
