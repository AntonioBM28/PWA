package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse extends GenericResponse {
    /** Se envia en cada peticion protegida como "Authorization: Bearer token". */
    private String token;
    private String tipoToken;
    private long expiraEnSegundos;
    private String usuario;
    private Rol rol;
    /** Nulo para un EJECUTIVO, que no es cliente. */
    private Integer clienteId;
}
