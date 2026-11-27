package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NipEstadoResponse extends GenericResponse {
    private boolean configurado;
    /** Si el NIP esta bloqueado por intentos fallidos, hasta cuando. */
    private OffsetDateTime bloqueadoHasta;
}
