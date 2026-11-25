package com.proyecto.servicios.enums;

/** Por que se cerro una sesion antes de expirar. */
public enum MotivoCierreSesion {
    /** El usuario cerro sesion. */
    LOGOUT,
    /** El usuario inicio sesion en otro dispositivo y eligio cerrar esta. */
    REEMPLAZADA
}
