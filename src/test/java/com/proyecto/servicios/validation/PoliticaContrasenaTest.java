package com.proyecto.servicios.validation;

import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaContrasenaTest {

    private final PoliticaContrasena politica = new PoliticaContrasena();

    @ParameterizedTest
    @ValueSource(strings = {"Segura#2026", "Abcdef1!", "Contraseña Larga 9"})
    void contrasenaValida(String password) {
        assertDoesNotThrow(() -> politica.validar(password));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Ab1!xyz|minimo 8 caracteres",
            "segura#2026|al menos una letra mayuscula",
            "SEGURA#2026|al menos una letra minuscula",
            "Segura#Clave|al menos un numero",
            "Segura2026|al menos un caracter especial"})
    void contrasenaInvalida_indicaLaReglaQueFalta(String password, String reglaFaltante) {
        ContrasenaInvalidaException ex = assertThrows(ContrasenaInvalidaException.class, () -> politica.validar(password));

        assertTrue(ex.getMessage().contains(reglaFaltante), ex.getMessage());
    }

    @Test
    void contrasenaNula_reportaTodasLasReglas() {
        ContrasenaInvalidaException ex = assertThrows(ContrasenaInvalidaException.class, () -> politica.validar(null));

        assertTrue(ex.getMessage().contains("minimo 8 caracteres"));
        assertTrue(ex.getMessage().contains("al menos un caracter especial"));
    }

    @Test
    void contrasenaMayorAlLimiteDeBcrypt() {
        String password = "Aa1!" + "x".repeat(PoliticaContrasena.BYTES_MAXIMOS_BCRYPT);

        ContrasenaInvalidaException ex = assertThrows(ContrasenaInvalidaException.class, () -> politica.validar(password));

        assertTrue(ex.getMessage().contains("maximo 72 caracteres"));
    }
}
