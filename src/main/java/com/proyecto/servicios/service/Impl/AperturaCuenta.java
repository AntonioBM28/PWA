package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Abre una cuenta para un cliente: estatus ACTIVA, saldo inicial definido por el sistema
 * (onboarding.cuenta.saldo-inicial) y numero de cuenta asignado por la secuencia de la base de
 * datos. La usan el registro de clientes y POST /cuentas; debe llamarse dentro de una transaccion.
 */
@Component
public class AperturaCuenta {

    private final CuentaRepository cuentaRepository;
    private final BigDecimal saldoInicial;

    public AperturaCuenta(CuentaRepository cuentaRepository,
                          @Value("${onboarding.cuenta.saldo-inicial}") BigDecimal saldoInicial) {
        if (saldoInicial.signum() < 0) {
            throw new IllegalStateException("onboarding.cuenta.saldo-inicial no puede ser negativo");
        }
        this.cuentaRepository = cuentaRepository;
        this.saldoInicial = saldoInicial;
    }

    public Cuenta abrir(Cliente cliente) {
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        cuenta.setSaldo(saldoInicial);
        return cuentaRepository.saveAndFlush(cuenta);
    }
}
