package com.proyecto.servicios.model.onboarding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActualizaClienteRequestValidationTest {

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
    void peticionVacia_esValidaPeroNoTraeCambios() {
        ActualizaClienteRequest request = new ActualizaClienteRequest();

        assertTrue(validator.validate(request).isEmpty());
        assertTrue(request.sinCambios());
    }

    @Test
    void camposOpcionalesVacios_sonValidosParaEliminarlos() {
        ActualizaClienteRequest request = new ActualizaClienteRequest();
        request.setSegundoNombre("");
        request.setTelefonoAlternativo("");
        ActualizaDomicilioRequest domicilio = new ActualizaDomicilioRequest();
        domicilio.setNumeroInterior("");
        request.setDomicilio(domicilio);

        assertTrue(validator.validate(request).isEmpty());
        assertFalse(request.sinCambios());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    void peticionInvalida_reportaElMensajeEsperado(String caso, Consumer<ActualizaClienteRequest> modificacion,
                                                   String mensajeEsperado) {
        ActualizaClienteRequest request = new ActualizaClienteRequest();
        modificacion.accept(request);

        Set<String> mensajes = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(mensajes.contains(mensajeEsperado), () -> caso + " -> " + mensajes);
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                caso("no se modifica la CURP", r -> r.setCurp("BAMJ900512HGTLRS09"), "La CURP no se puede modificar"),
                caso("no se modifica el RFC", r -> r.setRfc("BAMJ900512AB1"), "El RFC no se puede modificar"),
                caso("no se modifica el numero de cuenta", r -> r.setNumeroCuenta("1000000000"), "El numero de cuenta no se puede modificar"),
                caso("nombre vacio", r -> r.setNombre(""), "El nombre debe tener entre 2 y 50 caracteres"),
                caso("segundo nombre de 1 letra", r -> r.setSegundoNombre("A"),
                        "El segundo nombre debe tener entre 2 y 50 letras, o enviarse vacio para eliminarlo"),
                caso("telefono alternativo invalido", r -> r.setTelefonoAlternativo("123"),
                        "El telefono alternativo debe contener exactamente 10 digitos, o enviarse vacio para eliminarlo"),
                caso("ocupacion en blanco", r -> r.setOcupacion("   "), "La ocupacion no puede estar vacia"),
                caso("menor de edad", r -> r.setFechaNacimiento(LocalDate.now().minusYears(10)), "El cliente debe ser mayor de edad (18 anos o mas)"),
                caso("correo invalido", r -> r.setCorreo("correo@invalido"), "El correo electronico no tiene un formato valido"),
                caso("codigo postal invalido", r -> {
                    ActualizaDomicilioRequest domicilio = new ActualizaDomicilioRequest();
                    domicilio.setCodigoPostal("1234");
                    r.setDomicilio(domicilio);
                }, "El codigo postal debe contener exactamente 5 digitos"));
    }

    private static Arguments caso(String nombre, Consumer<ActualizaClienteRequest> modificacion, String mensaje) {
        return Arguments.of(nombre, modificacion, mensaje);
    }
}
