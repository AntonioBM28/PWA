package com.proyecto.servicios.security;

/**
 * Expresiones de @PreAuthorize. "acceso" es el bean ControlAcceso.
 */
public final class Permisos {

    /** Solo el personal de la institucion. */
    public static final String EJECUTIVO = "hasRole('EJECUTIVO')";

    /** Un EJECUTIVO, o el CLIENTE que busca por el correo de su propio JWT. */
    public static final String EJECUTIVO_O_CLIENTE_DEL_CORREO = EJECUTIVO + " or @acceso.esCorreoPropio(#filtro.correo)";

    /**
     * Un EJECUTIVO, o el CLIENTE dueno del recurso request.clienteId sin intentar cambiar su estatus:
     * la baja logica y la reactivacion son exclusivas del EJECUTIVO.
     */
    public static final String EJECUTIVO_O_CLIENTE_SIN_CAMBIO_DE_ESTATUS =
            EJECUTIVO + " or (@acceso.esCliente(#request.clienteId) and #request.cambios.activo == null)";

    /** Un EJECUTIVO, o el CLIENTE propietario de la cuenta cuyo saldo consulta. */
    public static final String EJECUTIVO_O_DUENO_DE_LA_CUENTA = EJECUTIVO + " or @acceso.esCuentaPropia(#request.numeroCuenta)";

    /** Un EJECUTIVO, o el CLIENTE que filtra sus cuentas por su correo o una cuenta propia. */
    public static final String EJECUTIVO_O_CLIENTE_DEL_FILTRO = EJECUTIVO
            + " or @acceso.esCorreoPropio(#filtro.correoCliente) or @acceso.esCuentaPropia(#filtro.numeroCuenta)";

    private Permisos() {
    }
}
