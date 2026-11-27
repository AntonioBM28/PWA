package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Autorizacion emitida: el token dinamico lo usa la app automaticamente (no se muestra al
 * cliente). Es de un solo uso, vence pronto y solo sirve para la compra autorizada.
 */
@Getter
@Setter
@NoArgsConstructor
public class AutorizacionTransaccionResponse extends GenericResponse {
    private UUID autorizacionId;
    /** Token de 8 digitos: la app lo usa al confirmar y nunca lo muestra. */
    private String tokenDinamico;
    private long vigenciaSegundos;
    private OffsetDateTime expiraEn;
}
