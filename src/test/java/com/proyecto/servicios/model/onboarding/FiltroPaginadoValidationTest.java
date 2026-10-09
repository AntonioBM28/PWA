package com.proyecto.servicios.model.onboarding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroPaginadoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void paginaExtrema_seRechazaEnLugarDeProvocarUnError500() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setPagina(Integer.MAX_VALUE);

        assertTrue(mensajes(filtro).contains("La pagina admite maximo 1000000"));
    }

    @Test
    void paginaMaxima_conTamanioMaximo_noDesbordaElDesplazamiento() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setPagina(FiltroPaginado.PAGINA_MAXIMA);
        filtro.setTamanio(FiltroPaginado.TAMANIO_MAXIMO);

        assertTrue(mensajes(filtro).isEmpty());
        assertTrue((long) filtro.getPagina() * filtro.getTamanio() < Integer.MAX_VALUE);
    }

    @Test
    void valoresNulos_usanLosValoresPorDefecto() {
        UsuarioFiltro filtro = new UsuarioFiltro();
        filtro.setPagina(null);
        filtro.setTamanio(null);

        assertEquals(0, filtro.getPagina());
        assertEquals(20, filtro.getTamanio());
    }

    private static Set<String> mensajes(Object filtro) {
        return validator.validate(filtro).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
