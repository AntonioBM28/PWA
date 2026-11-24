package com.proyecto.servicios.model.onboarding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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

    @Test
    void rangoDeFechasInvertido_seRechazaEnElRequest() {
        ClienteFiltro filtro = new ClienteFiltro();
        filtro.setFechaRegistroDesde(LocalDate.of(2026, 12, 31));
        filtro.setFechaRegistroHasta(LocalDate.of(2026, 1, 1));

        assertTrue(mensajes(filtro).contains("La fecha de registro inicial no puede ser posterior a la final"));
    }

    @Test
    void tamanioDePagina_esManipulableDentroDelLimitePermitido() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setTamanio(50);

        assertTrue(mensajes(filtro).isEmpty());
        assertEquals(50, filtro.getTamanio());
    }

    @Test
    void tamanioSuperiorAlMaximo_seRechaza() {
        CuentaFiltro filtro = new CuentaFiltro();
        filtro.setTamanio(101);

        assertTrue(mensajes(filtro).contains("El tamanio de pagina admite maximo 100"));
    }

    private static Set<String> mensajes(Object filtro) {
        return validator.validate(filtro).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
