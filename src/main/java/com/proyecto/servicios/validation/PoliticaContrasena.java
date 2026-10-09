package com.proyecto.servicios.validation;

import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de la contrasena: minimo 8 caracteres, al menos una mayuscula, una minuscula, un numero
 * y un caracter especial. El maximo de 72 bytes es el limite de BCrypt; lo que pase de ahi se
 * ignoraria al cifrar.
 */
@Component
public class PoliticaContrasena {

    static final int LONGITUD_MINIMA = 8;
    static final int BYTES_MAXIMOS_BCRYPT = 72;

    public void validar(String password) {
        List<String> faltantes = new ArrayList<>();
        if (password == null || password.length() < LONGITUD_MINIMA) {
            faltantes.add("minimo " + LONGITUD_MINIMA + " caracteres");
        }
        String valor = password == null ? "" : password;
        if (valor.chars().noneMatch(Character::isUpperCase)) {
            faltantes.add("al menos una letra mayuscula");
        }
        if (valor.chars().noneMatch(Character::isLowerCase)) {
            faltantes.add("al menos una letra minuscula");
        }
        if (valor.chars().noneMatch(Character::isDigit)) {
            faltantes.add("al menos un numero");
        }
        if (valor.chars().allMatch(Character::isLetterOrDigit)) {
            faltantes.add("al menos un caracter especial");
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_BCRYPT) {
            faltantes.add("maximo " + BYTES_MAXIMOS_BCRYPT + " caracteres");
        }
        if (!faltantes.isEmpty()) {
            throw new ContrasenaInvalidaException("La contrasena debe contener: " + String.join(", ", faltantes));
        }
    }
}
