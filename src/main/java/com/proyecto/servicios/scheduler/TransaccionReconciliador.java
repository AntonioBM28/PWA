package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.service.transaccion.TransaccionPersistenceService;
import com.proyecto.servicios.service.transaccion.TransaccionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.OffsetDateTime;

/** Consulta confirmTx sin repetir sendTx; conserva el débito mientras el estado sea incierto. */
@Component
@Slf4j
public class TransaccionReconciliador {
    private static final long VIGENCIA_CONFIRMACION_HORAS = 24;
    private final TransaccionPersistenceService persistencia;
    private final TransaccionService service;
    private final long esperaConfirmacionSegundos;

    public TransaccionReconciliador(TransaccionPersistenceService persistencia, TransaccionService service,
                                    @Value("${gestopago.transacciones.confirmacion-espera-segundos:72}") long esperaConfirmacionSegundos) {
        this.persistencia = persistencia;
        this.service = service;
        this.esperaConfirmacionSegundos = Math.max(62, esperaConfirmacionSegundos);
    }

    @Scheduled(fixedDelayString = "${gestopago.transacciones.conciliacion-intervalo-ms:60000}")
    public void reconciliar() {
        OffsetDateTime ahora = OffsetDateTime.now();
        var pendientes = persistencia.pendientesParaConfirmar(
                ahora.minusHours(VIGENCIA_CONFIRMACION_HORAS), ahora.minusSeconds(esperaConfirmacionSegundos));
        for (Transaccion tx : pendientes) {
            try {
                Transaccion resultado = service.confirmar(tx);
                if (resultado.getEstado() == EstadoTransaccion.CONCILIACION) {
                    log.info("Confirmación GestoPago aún indeterminada id={}", tx.getId());
                }
            } catch (RuntimeException ex) {
                log.warn("Fallo al procesar conciliación GestoPago id={} tipo={}",
                        tx.getId(), ex.getClass().getSimpleName());
            }
        }
    }
}
