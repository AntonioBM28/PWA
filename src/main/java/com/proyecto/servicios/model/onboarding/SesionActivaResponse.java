package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** 409 de POST /auth/login: el usuario ya tiene una sesion abierta en otro dispositivo. */
@Getter
@Setter
@NoArgsConstructor
public class SesionActivaResponse extends GenericResponse {
    private SesionActiva sesionActiva;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SesionActiva {
        private Plataforma plataforma;
        private OffsetDateTime iniciadaEn;
    }
}
