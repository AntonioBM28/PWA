package com.proyecto.servicios.enums;

import java.util.Arrays;

/**
 * Tabulador de ingreso mensual (en pesos). El cliente elige un rango en lugar de escribir
 * un monto libre.
 */
public enum RangoIngresoMensual {
    HASTA_5000,
    DE_5001_A_10000,
    DE_10001_A_20000,
    DE_20001_A_35000,
    DE_35001_A_50000,
    DE_50001_A_100000,
    MAS_DE_100000;

    /** true si el valor es el nombre de un rango del tabulador. */
    public static boolean existe(String valor) {
        return Arrays.stream(values()).anyMatch(r -> r.name().equals(valor));
    }
}
