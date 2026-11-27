package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.AbonoSaldo;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.onboarding.AbonoSaldoRequest;
import com.proyecto.servicios.model.onboarding.AbonoSaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.AbonoSaldoRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.AbonoSaldoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Abona saldo a una cuenta. La fila de la cuenta se bloquea durante el abono para que no se
 * pierda dinero si al mismo tiempo el cliente hace una compra (que descuenta el saldo).
 */
@Service
@Slf4j
public class AbonoSaldoServiceImpl implements AbonoSaldoService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Saldo abonado correctamente";

    private final CuentaRepository cuentaRepository;
    private final AbonoSaldoRepository abonoRepository;
    private final Clock reloj;

    public AbonoSaldoServiceImpl(CuentaRepository cuentaRepository, AbonoSaldoRepository abonoRepository,
                                 Clock reloj) {
        this.cuentaRepository = cuentaRepository;
        this.abonoRepository = abonoRepository;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public AbonoSaldoResponse abonar(AbonoSaldoRequest request, String ejecutivo) {
        String numeroCuenta = request.getNumeroCuenta();
        Cuenta cuenta = cuentaRepository.bloquearPorNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
        if (!Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            throw new ClienteInactivoException("No se puede abonar: el cliente esta dado de baja");
        }
        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new ClienteInactivoException("No se puede abonar: la cuenta " + numeroCuenta + " no esta activa");
        }

        BigDecimal monto = request.getMonto().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior.add(monto);
        cuenta.setSaldo(saldoNuevo);

        AbonoSaldo abono = new AbonoSaldo();
        abono.setId(UUID.randomUUID());
        abono.setCuentaId(cuenta.getId());
        abono.setMonto(monto);
        abono.setSaldoAnterior(saldoAnterior);
        abono.setSaldoNuevo(saldoNuevo);
        abono.setConcepto(request.getConcepto());
        abono.setEjecutivo(ejecutivo);
        abono.setFechaCreacion(OffsetDateTime.now(reloj));
        abonoRepository.save(abono);
        log.info("Abono de saldo: cuenta id={}, monto={}, ejecutivo={}", cuenta.getId(), monto, ejecutivo);

        AbonoSaldoResponse response = new AbonoSaldoResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setAbonoId(abono.getId());
        response.setNumeroCuenta(numeroCuenta);
        response.setMonto(monto);
        response.setSaldoAnterior(saldoAnterior);
        response.setSaldo(saldoNuevo);
        response.setFecha(abono.getFechaCreacion());
        return response;
    }
}
