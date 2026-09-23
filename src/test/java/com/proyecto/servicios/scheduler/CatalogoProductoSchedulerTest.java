package com.proyecto.servicios.scheduler;

import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.service.CatalogoProductoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoProductoSchedulerTest {

    @Mock
    private CatalogoProductoService catalogoProductoService;

    @Test
    void sincronizarAlIniciar_habilitado_sincroniza() {
        new CatalogoProductoScheduler(catalogoProductoService, true).sincronizarAlIniciar();

        verify(catalogoProductoService).sincronizarCatalogo();
    }

    @Test
    void sincronizarAlIniciar_deshabilitado_noSincroniza() {
        new CatalogoProductoScheduler(catalogoProductoService, false).sincronizarAlIniciar();

        verifyNoInteractions(catalogoProductoService);
    }

    @Test
    void sincronizarProgramado_errorDeIntegracion_noPropagaExcepcion() {
        when(catalogoProductoService.sincronizarCatalogo())
                .thenThrow(new GestoPagoTimeoutException("Tiempo de espera agotado al invocar GestoPago", null));

        assertDoesNotThrow(() -> new CatalogoProductoScheduler(catalogoProductoService, true).sincronizarProgramado());
    }
}
