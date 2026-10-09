package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.onboarding.ActualizaCuentaRequest;
import com.proyecto.servicios.model.onboarding.CrearCuentaRequest;
import com.proyecto.servicios.model.onboarding.CuentaDetalleResponse;
import com.proyecto.servicios.model.onboarding.CuentaFiltro;
import com.proyecto.servicios.model.onboarding.CuentasResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    private static final String NUMERO_CUENTA = "1000000000";
    private static final Integer ID_CLIENTE = 1;
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("100.00");

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private CuentaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CuentaServiceImpl(cuentaRepository, clienteRepository,
                new AperturaCuenta(cuentaRepository, SALDO_INICIAL), Mappers.getMapper(CuentaMapper.class));
    }

    // ===================================================================== Consultas

    @Test
    void consultarCuenta_existente() {
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA))
                .thenReturn(Optional.of(cuenta(cliente(true), EstatusCuenta.ACTIVA)));

        CuentaDetalleResponse response = service.consultarCuenta(NUMERO_CUENTA);

        assertEquals(NUMERO_CUENTA, response.getCuenta().getNumeroCuenta());
        assertEquals(ID_CLIENTE, response.getCuenta().getClienteId());
    }

    @Test
    void consultarCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta("9999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> service.consultarCuenta("9999999999"));
    }

    @Test
    void consultarCuentas_porClienteYEstatus_devuelvePagina() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setClienteId(ID_CLIENTE);
        filtro.setEstatus(EstatusCuenta.ACTIVA);
        when(clienteRepository.existsById(ID_CLIENTE)).thenReturn(true);
        when(cuentaRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(inv ->
                new PageImpl<>(List.of(cuenta(cliente(true), EstatusCuenta.ACTIVA)), inv.getArgument(1), 1));

        CuentasResponse response = service.consultarCuentas(filtro);

        assertEquals(1, response.getCuentas().size());
        assertEquals(1, response.getTotalElementos());
        assertEquals(20, response.getTamanio());
    }

    @Test
    void consultarCuentas_deClienteInexistente() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setClienteId(99);
        when(clienteRepository.existsById(99)).thenReturn(false);

        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarCuentas(filtro));
        verify(cuentaRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void consultarSaldo_existente() {
        when(cuentaRepository.consultarSaldo(NUMERO_CUENTA)).thenReturn(Optional.of(new BigDecimal("5000.00")));

        SaldoResponse response = service.consultarSaldo(NUMERO_CUENTA);

        assertEquals(new BigDecimal("5000.00"), response.getSaldo());
        assertEquals(NUMERO_CUENTA, response.getNumeroCuenta());
    }

    @Test
    void consultarSaldo_cuentaInexistente() {
        when(cuentaRepository.consultarSaldo(NUMERO_CUENTA)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> service.consultarSaldo(NUMERO_CUENTA));
    }

    // ===================================================================== Apertura

    @Test
    void crearCuenta_clienteActivo_abreCuentaActivaConSaldoInicial() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(cuentaRepository.saveAndFlush(any(Cuenta.class))).thenAnswer(inv -> {
            Cuenta cuenta = inv.getArgument(0);
            cuenta.setNumeroCuenta("1000000002");
            return cuenta;
        });

        CuentaDetalleResponse response = service.crearCuenta(crearRequest());

        assertEquals(CuentaServiceImpl.MENSAJE_CREACION, response.getMensaje());
        assertEquals("1000000002", response.getCuenta().getNumeroCuenta());
        assertEquals(EstatusCuenta.ACTIVA, response.getCuenta().getEstatus());
        assertEquals(SALDO_INICIAL, response.getCuenta().getSaldo());
        assertEquals(ID_CLIENTE, response.getCuenta().getClienteId());
    }

    @Test
    void crearCuenta_clienteInactivo_seRechaza() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente(false)));

        assertThrows(ClienteInactivoException.class, () -> service.crearCuenta(crearRequest()));
        verify(cuentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crearCuenta_clienteInexistente() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.crearCuenta(crearRequest()));
    }

    @Test
    void aperturaCuenta_saldoInicialNegativo_impideIniciar() {
        BigDecimal negativo = new BigDecimal("-0.01");

        assertThrows(IllegalStateException.class, () -> new AperturaCuenta(cuentaRepository, negativo));
    }

    // ===================================================================== Cambio de estatus

    @Test
    void actualizarCuenta_activarCuentaDeClienteActivo() {
        Cuenta cuenta = cuenta(cliente(true), EstatusCuenta.INACTIVA);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.saveAndFlush(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaDetalleResponse response = service.actualizarCuenta(NUMERO_CUENTA, estatus(EstatusCuenta.ACTIVA));

        assertEquals(EstatusCuenta.ACTIVA, response.getCuenta().getEstatus());
        assertEquals(CuentaServiceImpl.MENSAJE_ACTUALIZACION, response.getMensaje());
    }

    @Test
    void actualizarCuenta_activarCuentaDeClienteInactivo_seRechaza() {
        Cuenta cuenta = cuenta(cliente(false), EstatusCuenta.INACTIVA);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        assertThrows(ClienteInactivoException.class,
                () -> service.actualizarCuenta(NUMERO_CUENTA, estatus(EstatusCuenta.ACTIVA)));
        verify(cuentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarCuenta_inactivarCuentaDeClienteInactivo_sePermite() {
        Cuenta cuenta = cuenta(cliente(false), EstatusCuenta.ACTIVA);
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.saveAndFlush(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizarCuenta(NUMERO_CUENTA, estatus(EstatusCuenta.INACTIVA));

        ArgumentCaptor<Cuenta> captor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).saveAndFlush(captor.capture());
        assertEquals(EstatusCuenta.INACTIVA, captor.getValue().getEstatus());
    }

    @Test
    void actualizarCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> service.actualizarCuenta(NUMERO_CUENTA, estatus(EstatusCuenta.ACTIVA)));
    }

    private static CrearCuentaRequest crearRequest() {
        CrearCuentaRequest request = new CrearCuentaRequest();
        request.setClienteId(ID_CLIENTE);
        return request;
    }

    private static ActualizaCuentaRequest estatus(EstatusCuenta estatus) {
        ActualizaCuentaRequest request = new ActualizaCuentaRequest();
        request.setEstatus(estatus);
        return request;
    }

    private static Cliente cliente(boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setId(ID_CLIENTE);
        cliente.setActivo(activo);
        return cliente;
    }

    private static Cuenta cuenta(Cliente cliente, EstatusCuenta estatus) {
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(NUMERO_CUENTA);
        cuenta.setEstatus(estatus);
        cuenta.setSaldo(SALDO_INICIAL);
        return cuenta;
    }
}
