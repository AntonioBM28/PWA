package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.AbonoSaldoRequest;
import com.proyecto.servicios.model.onboarding.AbonoSaldoResponse;

/** Abonos de saldo a las cuentas: la unica forma de aumentar el saldo de un cliente. */
public interface AbonoSaldoService {

    /**
     * Abona el monto a la cuenta y deja el registro de auditoria.
     *
     * @param ejecutivo correo del ejecutivo que hace el abono
     */
    AbonoSaldoResponse abonar(AbonoSaldoRequest request, String ejecutivo);
}
