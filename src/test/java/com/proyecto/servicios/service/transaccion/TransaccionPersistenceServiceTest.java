package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransaccionPersistenceServiceTest {

    private CuentaRepository cuentaRepository;
    private TransaccionRepository transaccionRepository;
    private TransaccionPersistenceService service;

    private Cliente cliente;
    private Cuenta cuenta;

    @BeforeEach
    void setUp() {
        cuentaRepository = mock(CuentaRepository.class);
        transaccionRepository = mock(TransaccionRepository.class);
        service = new TransaccionPersistenceService(cuentaRepository, transaccionRepository);

        cliente = new Cliente();
        cliente.setId(10);
        cliente.setActivo(true);

        cuenta = new Cuenta();
        cuenta.setId(100);
        cuenta.setNumeroCuenta("0123456789");
        cuenta.setCliente(cliente);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        cuenta.setSaldo(new BigDecimal("500.00"));
    }

    // ─────────────────────────────────────────────────────────────
    // reservar
    // ─────────────────────────────────────────────────────────────

    @Test
    void reservarFallaSiCuentaNoExiste() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.empty());

        TransaccionException ex = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void reservarFallaSiNoEsEjecutivoYClienteNoAutorizado() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));

        // clienteAutenticado distinto
        TransaccionException ex1 = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 99, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));
        assertEquals(HttpStatus.FORBIDDEN, ex1.getHttpStatus());

        // clienteAutenticado null
        TransaccionException ex2 = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", null, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));
        assertEquals(HttpStatus.FORBIDDEN, ex2.getHttpStatus());
    }

    @Test
    void reservarDevuelveReservaExistenteSiMismaHuella() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));

        Transaccion existente = new Transaccion();
        existente.setId(UUID.randomUUID());
        existente.setHuellaSolicitud("huella-1");
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1"))
                .thenReturn(Optional.of(existente));

        var reserva = service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00"));

        assertTrue(reserva.repetida());
        assertEquals(existente, reserva.transaccion());
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void reservarFallaSiClaveExisteConHuellaDistinta() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));

        Transaccion existente = new Transaccion();
        existente.setId(UUID.randomUUID());
        existente.setHuellaSolicitud("otra-huella");
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1"))
                .thenReturn(Optional.of(existente));

        TransaccionException ex = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));

        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void reservarFallaSiClienteNoEstaActivo() {
        cliente.setActivo(false);
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1")).thenReturn(Optional.empty());

        TransaccionException ex = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void reservarFallaSiCuentaNoEstaActiva() {
        cuenta.setEstatus(EstatusCuenta.INACTIVA);
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1")).thenReturn(Optional.empty());

        TransaccionException ex = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void reservarFallaSiSaldoInsuficienteONulo() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1")).thenReturn(Optional.empty());

        cuenta.setSaldo(new BigDecimal("50.00"));
        TransaccionException ex1 = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex1.getHttpStatus());

        cuenta.setSaldo(null);
        TransaccionException ex2 = assertThrows(TransaccionException.class, () ->
                service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00")));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex2.getHttpStatus());
    }

    @Test
    void reservarExitosoComoClienteDebitaSaldoYGuardaTransaccion() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1")).thenReturn(Optional.empty());

        var reserva = service.reservar("0123456789", 10, false, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00"));

        assertFalse(reserva.repetida());
        assertNotNull(reserva.transaccion());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
        assertEquals(EstadoTransaccion.PENDIENTE, reserva.transaccion().getEstado());
        verify(transaccionRepository).saveAndFlush(any(Transaccion.class));
    }

    @Test
    void reservarExitosoComoEjecutivoSinImportarClienteAutenticado() {
        when(cuentaRepository.bloquearPorNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.findByClienteIdAndClaveIdempotencia(10, "clave-1")).thenReturn(Optional.empty());

        var reserva = service.reservar("0123456789", 999, true, "clave-1", "huella-1", "upc-1", "rnd-1", "enc-1", new BigDecimal("100.00"));

        assertFalse(reserva.repetida());
        assertEquals(new BigDecimal("400.00"), cuenta.getSaldo());
    }

    // ─────────────────────────────────────────────────────────────
    // buscarPorClave
    // ─────────────────────────────────────────────────────────────

    @Test
    void buscarPorClaveFallaSiCuentaNoExiste() {
        when(cuentaRepository.findByNumeroCuenta("0123456789")).thenReturn(Optional.empty());

        assertThrows(TransaccionException.class, () ->
                service.buscarPorClave("0123456789", 10, false, "clave-1"));
    }

    @Test
    void buscarPorClaveFallaSiNoEsEjecutivoYNoCoincideCliente() {
        when(cuentaRepository.findByNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));

        assertThrows(TransaccionException.class, () ->
                service.buscarPorClave("0123456789", 99, false, "clave-1"));
    }

    @Test
    void buscarPorClaveExitoso() {
        when(cuentaRepository.findByNumeroCuenta("0123456789")).thenReturn(Optional.of(cuenta));
        Transaccion tx = new Transaccion();
        when(transaccionRepository.buscarPorClave(10, "clave-1")).thenReturn(Optional.of(tx));

        Optional<Transaccion> result = service.buscarPorClave("0123456789", 10, false, "clave-1");

        assertTrue(result.isPresent());
        assertEquals(tx, result.get());
    }

    // ─────────────────────────────────────────────────────────────
    // finalizar
    // ─────────────────────────────────────────────────────────────

    @Test
    void finalizarFallaSiTransaccionNoExiste() {
        UUID id = UUID.randomUUID();
        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.empty());

        assertThrows(TransaccionException.class, () ->
                service.finalizar(id, EstadoTransaccion.APROBADA, "01", "Ok", "tx-1", "auth-1", "pin", "folio", "leyenda"));
    }

    @Test
    void finalizarRetornaMismaTxSiYaEstabaAprobadaORechazada() {
        UUID id = UUID.randomUUID();
        Transaccion aprobada = new Transaccion();
        aprobada.setId(id);
        aprobada.setEstado(EstadoTransaccion.APROBADA);
        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(aprobada));

        Transaccion res1 = service.finalizar(id, EstadoTransaccion.RECHAZADA, "99", "msg", null, null, null, null, null);
        assertSame(aprobada, res1);

        Transaccion rechazada = new Transaccion();
        rechazada.setId(id);
        rechazada.setEstado(EstadoTransaccion.RECHAZADA);
        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(rechazada));

        Transaccion res2 = service.finalizar(id, EstadoTransaccion.APROBADA, "01", "msg", null, null, null, null, null);
        assertSame(rechazada, res2);

        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void finalizarRechazadaReembolsaSaldo() {
        UUID id = UUID.randomUUID();
        Transaccion pendiente = new Transaccion();
        pendiente.setId(id);
        pendiente.setCuentaId(100);
        pendiente.setMonto(new BigDecimal("100.00"));
        pendiente.setEstado(EstadoTransaccion.PENDIENTE);

        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(pendiente));
        when(cuentaRepository.findById(100)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaccion res = service.finalizar(id, EstadoTransaccion.RECHAZADA, "99", "error", null, null, null, null, null);

        assertEquals(EstadoTransaccion.RECHAZADA, res.getEstado());
        assertEquals(new BigDecimal("600.00"), cuenta.getSaldo()); // 500 + 100 reembolsado
    }

    @Test
    void finalizarRechazadaFallaSiCuentaAsociadaNoExiste() {
        UUID id = UUID.randomUUID();
        Transaccion pendiente = new Transaccion();
        pendiente.setId(id);
        pendiente.setCuentaId(999);
        pendiente.setMonto(new BigDecimal("100.00"));
        pendiente.setEstado(EstadoTransaccion.PENDIENTE);

        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(pendiente));
        when(cuentaRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                service.finalizar(id, EstadoTransaccion.RECHAZADA, "99", "error", null, null, null, null, null));
    }

    @Test
    void finalizarAprobadaLimitaCamposLargos() {
        UUID id = UUID.randomUUID();
        Transaccion pendiente = new Transaccion();
        pendiente.setId(id);
        pendiente.setCuentaId(100);
        pendiente.setMonto(new BigDecimal("100.00"));
        pendiente.setEstado(EstadoTransaccion.PENDIENTE);

        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(pendiente));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String mensajeLargo = "A".repeat(600);
        String idTxLargo = "B".repeat(50);
        String authLargo = "C".repeat(80);
        String pinLargo = "D".repeat(300);
        String folioLargo = "E".repeat(300);
        String leyendaLarga = "F".repeat(6000);

        Transaccion res = service.finalizar(id, EstadoTransaccion.APROBADA, "01", mensajeLargo, idTxLargo, authLargo, pinLargo, folioLargo, leyendaLarga);

        assertEquals(EstadoTransaccion.APROBADA, res.getEstado());
        assertEquals(500, res.getMensajeProveedor().length());
        assertEquals(32, res.getIdTx().length());
        assertEquals(60, res.getNumeroAutorizacion().length());
        assertEquals(250, res.getPin().length());
        assertEquals(250, res.getFolio().length());
        assertEquals(5000, res.getLeyenda().length());
    }

    // ─────────────────────────────────────────────────────────────
    // dejarEnConciliacion
    // ─────────────────────────────────────────────────────────────

    @Test
    void dejarEnConciliacionFallaSiNoExiste() {
        UUID id = UUID.randomUUID();
        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.empty());

        assertThrows(TransaccionException.class, () ->
                service.dejarEnConciliacion(id, "06", "mensaje"));
    }

    @Test
    void dejarEnConciliacionActualizaSiEstadoEsPendienteOConciliacion() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setEstado(EstadoTransaccion.PENDIENTE);

        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaccion res = service.dejarEnConciliacion(id, "1234567890", "A".repeat(600));

        assertEquals(EstadoTransaccion.CONCILIACION, res.getEstado());
        assertEquals(8, res.getCodigoProveedor().length()); // limitado a 8
        assertEquals(500, res.getMensajeProveedor().length()); // limitado a 500
    }

    @Test
    void dejarEnConciliacionNoModificaSiYaFinalizada() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setEstado(EstadoTransaccion.APROBADA);

        when(transaccionRepository.bloquearPorId(id)).thenReturn(Optional.of(tx));

        Transaccion res = service.dejarEnConciliacion(id, "06", "msg");

        assertSame(tx, res);
        verify(transaccionRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // buscar
    // ─────────────────────────────────────────────────────────────

    @Test
    void buscarFallaSiNoExiste() {
        UUID id = UUID.randomUUID();
        when(transaccionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransaccionException.class, () -> service.buscar(id, 10, false));
    }

    @Test
    void buscarFallaSiClienteNoAutorizado() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setClienteId(10);
        when(transaccionRepository.findById(id)).thenReturn(Optional.of(tx));

        assertThrows(TransaccionException.class, () -> service.buscar(id, 99, false));
    }

    @Test
    void buscarExitosoParaClienteYParaEjecutivo() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setClienteId(10);
        when(transaccionRepository.findById(id)).thenReturn(Optional.of(tx));

        // Cliente propio
        assertSame(tx, service.buscar(id, 10, false));
        // Ejecutivo sobre cualquier cliente
        assertSame(tx, service.buscar(id, 999, true));
    }

    // ─────────────────────────────────────────────────────────────
    // pendientesParaConfirmar
    // ─────────────────────────────────────────────────────────────

    @Test
    void pendientesParaConfirmarInvocaRepositorio() {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(1);
        OffsetDateTime hasta = OffsetDateTime.now();
        List<Transaccion> lista = List.of(new Transaccion());

        when(transaccionRepository.findTop100ByEstadoInAndFechaCreacionBetweenOrderByFechaCreacionAsc(
                List.of(EstadoTransaccion.PENDIENTE, EstadoTransaccion.CONCILIACION), desde, hasta))
                .thenReturn(lista);

        List<Transaccion> res = service.pendientesParaConfirmar(desde, hasta);

        assertEquals(lista, res);
    }

    // ─────────────────────────────────────────────────────────────
    // marcarPendiente
    // ─────────────────────────────────────────────────────────────

    @Test
    void marcarPendienteFallaSiNoExiste() {
        UUID id = UUID.randomUUID();
        when(transaccionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.marcarPendiente(id));
    }

    @Test
    void marcarPendienteCambiaAConciliacionSiPendienteOConciliacion() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setEstado(EstadoTransaccion.PENDIENTE);

        when(transaccionRepository.findById(id)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaccion res = service.marcarPendiente(id);

        assertEquals(EstadoTransaccion.CONCILIACION, res.getEstado());
    }

    @Test
    void marcarPendienteNoModificaSiYaAprobada() {
        UUID id = UUID.randomUUID();
        Transaccion tx = new Transaccion();
        tx.setId(id);
        tx.setEstado(EstadoTransaccion.APROBADA);

        when(transaccionRepository.findById(id)).thenReturn(Optional.of(tx));

        Transaccion res = service.marcarPendiente(id);

        assertSame(tx, res);
        verify(transaccionRepository, never()).save(any());
    }
}
