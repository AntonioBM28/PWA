package com.proyecto.servicios.security;

/**
 * Expresiones de @PreAuthorize. "acceso" es el bean ControlAcceso.
 */
public final class Permisos {

    /** Solo el personal de la institucion. */
    public static final String EJECUTIVO = "hasRole('EJECUTIVO')";

    /** Un EJECUTIVO, o el CLIENTE dueno del recurso #id. */
    public static final String EJECUTIVO_O_CLIENTE_DEL_ID = EJECUTIVO + " or @acceso.esCliente(#id)";

    /**
     * Un EJECUTIVO, o el CLIENTE dueno del recurso #id siempre que no intente cambiar su estatus:
     * la baja logica y la reactivacion son exclusivas del EJECUTIVO.
     */
    public static final String EJECUTIVO_O_CLIENTE_SIN_CAMBIO_DE_ESTATUS =
            EJECUTIVO + " or (@acceso.esCliente(#id) and #request.activo == null)";

    /** Un EJECUTIVO, o el CLIENTE dueno de la cuenta #numeroCuenta. */
    public static final String EJECUTIVO_O_DUENO_DE_LA_CUENTA = EJECUTIVO + " or @acceso.esCuentaPropia(#numeroCuenta)";

    /** Un EJECUTIVO, o el CLIENTE que consulta sus propias cuentas (filtro clienteId). */
    public static final String EJECUTIVO_O_CLIENTE_DEL_FILTRO = EJECUTIVO + " or @acceso.esCliente(#filtro.clienteId)";

    private Permisos() {
    }
}
