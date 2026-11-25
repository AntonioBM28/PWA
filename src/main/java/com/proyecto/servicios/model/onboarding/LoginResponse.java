package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesion iniciada (segundo paso, despues de verificar el codigo). */
@Getter
@Setter
@NoArgsConstructor
public class LoginResponse extends GenericResponse {
    /**
     * Solo en MOVIL: se envia en cada peticion protegida como "Authorization: Bearer token". En WEB
     * se omite, porque el token viaja en una cookie HttpOnly que JavaScript no puede leer.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tipoToken;
    private long expiraEnSegundos;
    private String usuario;
    private Rol rol;
    /** Nulo para un EJECUTIVO, que no es cliente. */
    private Integer clienteId;
}
