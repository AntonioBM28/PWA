package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ActualizaCuentaRequest;
import com.proyecto.servicios.model.onboarding.CrearCuentaRequest;
import com.proyecto.servicios.model.onboarding.CuentaDetalleResponse;
import com.proyecto.servicios.model.onboarding.CuentaFiltro;
import com.proyecto.servicios.model.onboarding.CuentasResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;

public interface CuentaService {

    CuentaDetalleResponse consultarCuenta(String numeroCuenta);

    CuentasResponse consultarCuentas(CuentaFiltro filtro);

    SaldoResponse consultarSaldo(String numeroCuenta);

    CuentaDetalleResponse crearCuenta(CrearCuentaRequest request);

    CuentaDetalleResponse actualizarCuenta(String numeroCuenta, ActualizaCuentaRequest request);
}
