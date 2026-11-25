package com.proyecto.servicios.enums;

import java.util.Arrays;

/** Catalogo de ocupaciones que puede elegir un cliente. */
public enum Ocupacion {
    EMPLEADO_SECTOR_PRIVADO,
    SERVIDOR_PUBLICO,
    PROFESIONISTA_INDEPENDIENTE,
    EMPRESARIO,
    COMERCIANTE,
    TRABAJADOR_POR_CUENTA_PROPIA,
    OFICIO_TECNICO,
    DOCENTE,
    PROFESIONAL_DE_LA_SALUD,
    FUERZAS_ARMADAS_SEGURIDAD,
    AGROPECUARIO,
    ESTUDIANTE,
    HOGAR,
    JUBILADO_PENSIONADO,
    DESEMPLEADO,
    OTRA;

    /** true si el valor es el nombre de una ocupacion del catalogo. */
    public static boolean existe(String valor) {
        return Arrays.stream(values()).anyMatch(o -> o.name().equals(valor));
    }
}
