package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Primer paso del inicio de sesion: la contrasena fue correcta y se envio un codigo por correo.
 * Todavia no hay sesion; la app debe enviar el codigo junto con desafioId.
 */
@Getter
@Setter
@NoArgsConstructor
public class DesafioVerificacionResponse extends GenericResponse {
    private UUID desafioId;
    /** Correo al que se envio el codigo, parcialmente oculto (an***@gmail.com). */
    private String correoEnmascarado;
    private long expiraEnSegundos;
    /** Segundos que faltan para poder pedir otro codigo. */
    private long reenviarEnSegundos;
}
