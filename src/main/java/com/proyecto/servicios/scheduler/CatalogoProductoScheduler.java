package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.service.CatalogoProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara la sincronizacion del catalogo GestoPago -> MongoDB todos los dias a la hora
 * configurada y, opcionalmente, al arrancar la aplicacion.
 */
@Component
@Slf4j
public class CatalogoProductoScheduler {

    private final CatalogoProductoService catalogoProductoService;
    private final boolean cargaAlIniciar;

    public CatalogoProductoScheduler(CatalogoProductoService catalogoProductoService,
                                     @Value("${gestopago.productos.carga-al-iniciar:true}") boolean cargaAlIniciar) {
        this.catalogoProductoService = catalogoProductoService;
        this.cargaAlIniciar = cargaAlIniciar;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAlIniciar() {
        if (cargaAlIniciar) {
            sincronizar();
        }
    }

    @Scheduled(cron = "${gestopago.productos.cron}", zone = "${gestopago.productos.cron-zona}")
    public void sincronizarProgramado() {
        sincronizar();
    }

    private void sincronizar() {
        log.info("Inicio sincronizacion del catalogo de productos");
        try {
            int total = catalogoProductoService.sincronizarCatalogo();
            log.info("Fin sincronizacion del catalogo de productos: {} productos", total);
        } catch (RuntimeException e) {
            // El detalle ya se registro en la capa de servicio; se conserva el catalogo anterior.
            log.error("Sincronizacion del catalogo fallida, se conserva el catalogo anterior: tipo={}",
                    e.getClass().getSimpleName());
        }
    }
}
