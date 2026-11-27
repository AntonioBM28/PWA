package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.AbonoSaldo;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.onboarding.AbonoSaldoRequest;
import com.proyecto.servicios.model.onboarding.AbonoSaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.AbonoSaldoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonoSaldoServiceImplTest {

    private static final String NUMERO_CUENTA = "1000000009";
    private static final String EJECUTIVO = "ejecutivo@mail.com";

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private AbonoSaldoRepository abonoRepository;

    private AbonoSaldoServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-26T18:00:00Z"), ZoneOffset.UTC);
        service = new AbonoSaldoServiceImpl(cuentaRepository, abonoRepository, reloj);
    }

    @Test
    void abonar_sumaAlSaldoYRegistraElAbono() {
        Cuenta cuenta = cuenta(true, EstatusCuenta.ACTIVA, "250.50");
        when(cuentaRepository.bloquearPorNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        AbonoSaldoResponse response = service.abonar(request("1000"), EJECUTIVO);

        assertEquals(new BigDecimal("1250.50"), cuenta.getSaldo());
        assertEquals(new BigDecimal("1250.50"), response.getSaldo());
        assertEquals(new BigDecimal("250.50"), response.getSaldoAnterior());
        assertEquals(new BigDecimal("1000.00"), response.getMonto());

        ArgumentCaptor<AbonoSaldo> abono = ArgumentCaptor.forClass(AbonoSaldo.class);
        verify(abonoRepository).save(abono.capture());
        assertEquals(EJECUTIVO, abono.getValue().getEjecutivo());
        assertEquals(cuenta.getId(), abono.getValue().getCuentaId());
        assertEquals("Deposito en sucursal", abono.getValue().getConcepto());
        assertEquals(new BigDecimal("1250.50"), abono.getValue().getSaldoNuevo());
    }

    @Test
    void abonar_cuentaInexistente() {
        when(cuentaRepository.bloquearPorNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class, () -> service.abonar(request("100"), EJECUTIVO));
    }

    @Test
    void abonar_clienteDadoDeBaja_noModificaElSaldo() {
        Cuenta cuenta = cuenta(false, EstatusCuenta.INACTIVA, "0.00");
        when(cuentaRepository.bloquearPorNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        assertThrows(ClienteInactivoException.class, () -> service.abonar(request("100"), EJECUTIVO));
        assertEquals(new BigDecimal("0.00"), cuenta.getSaldo());
        verify(abonoRepository, never()).save(any());
    }

    @Test
    void abonar_cuentaInactiva() {
        Cuenta cuenta = cuenta(true, EstatusCuenta.INACTIVA, "0.00");
        when(cuentaRepository.bloquearPorNumeroCuenta(NUMERO_CUENTA)).thenReturn(Optional.of(cuenta));

        assertThrows(ClienteInactivoException.class, () -> service.abonar(request("100"), EJECUTIVO));
        verify(abonoRepository, never()).save(any());
    }

    @Test
    void request_validaMontoYConcepto() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            assertTrue(validator.validate(request("1500.50")).isEmpty());
            assertTrue(mensajes(validator, request("0.99")).contains("El monto minimo de un abono es de $1.00"));
            assertTrue(mensajes(validator, request("50000.01")).contains("El monto maximo de un abono es de $50,000.00"));
            assertTrue(mensajes(validator, request("10.123")).contains("El monto admite maximo 2 decimales"));

            AbonoSaldoRequest conScript = request("10");
            conScript.setConcepto("<script>");
            assertTrue(mensajes(validator, conScript).stream().anyMatch(m -> m.startsWith("El concepto")));

            AbonoSaldoRequest sinConcepto = request("10");
            sinConcepto.setConcepto("   ");
            assertEquals(null, sinConcepto.getConcepto());
        }
    }

    private static Set<String> mensajes(Validator validator, AbonoSaldoRequest request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static AbonoSaldoRequest request(String monto) {
        AbonoSaldoRequest request = new AbonoSaldoRequest();
        request.setNumeroCuenta(NUMERO_CUENTA);
        request.setMonto(new BigDecimal(monto));
        request.setConcepto("Deposito en sucursal");
        return request;
    }

    private static Cuenta cuenta(boolean clienteActivo, EstatusCuenta estatus, String saldo) {
        Cliente cliente = new Cliente();
        cliente.setId(7);
        cliente.setActivo(clienteActivo);
        Cuenta cuenta = new Cuenta();
        cuenta.setId(3);
        cuenta.setCliente(cliente);
        cuenta.setEstatus(estatus);
        cuenta.setSaldo(new BigDecimal(saldo));
        return cuenta;
    }
}
