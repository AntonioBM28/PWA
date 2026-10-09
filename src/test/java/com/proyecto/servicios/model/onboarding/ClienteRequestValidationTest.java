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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteRequestValidationTest {

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
    void requestValido_sinErrores() {
        assertTrue(validator.validate(ClienteRequestFixture.requestValido()).isEmpty());
    }

    @Test
    void curpRfcYCorreo_seNormalizanAlRecibirlos() {
        ClienteRequest request = ClienteRequestFixture.requestValido();
        request.setCurp(" bamj900512hgtlrs09 ");
        request.setRfc("bamj900512ab1");
        request.setCorreo(" Jose.Balderas@Mail.COM ");

        assertEquals("BAMJ900512HGTLRS09", request.getCurp());
        assertEquals("BAMJ900512AB1", request.getRfc());
        assertEquals("jose.balderas@mail.com", request.getCorreo());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void textoLibreConPuntuacionComun_esValido() {
        ClienteRequest request = ClienteRequestFixture.requestValido();
        request.setEmpresa("AT&T Comunicaciones, S.A. de C.V. (Mexico)");
        request.setOcupacion("Ingeniera de software - Lider tecnica");
        request.getDomicilio().setCalle("Av. 16 de Septiembre #120");
        request.getDomicilio().setNumeroExterior("12-B");
        request.getDomicilio().setNumeroInterior("Depto. 4");
        request.getDomicilio().setColonia("1° de Mayo");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void segundoNombreYTelefonoAlternativo_sonOpcionales() {
        ClienteRequest request = ClienteRequestFixture.requestValido();
        request.setSegundoNombre(null);
        request.setTelefonoAlternativo(null);
        request.getDomicilio().setNumeroInterior(null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    void requestInvalido_reportaElMensajeEsperado(String caso, Consumer<ClienteRequest> modificacion, String mensajeEsperado) {
        ClienteRequest request = ClienteRequestFixture.requestValido();
        modificacion.accept(request);

        Set<String> mensajes = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(mensajes.contains(mensajeEsperado), () -> caso + " -> " + mensajes);
    }

    static Stream<Arguments> casosInvalidos() {
        return Stream.of(
                caso("nombre obligatorio", r -> r.setNombre(null), "El nombre es obligatorio"),
                caso("nombre con numeros", r -> r.setNombre("J0se"), "El nombre solo admite letras y espacios"),
                caso("nombre de 1 caracter", r -> r.setNombre("J"), "El nombre debe tener entre 2 y 50 caracteres"),
                caso("nombre de 51 caracteres", r -> r.setNombre("A".repeat(51)), "El nombre debe tener entre 2 y 50 caracteres"),
                caso("empresa con <script>", r -> r.setEmpresa("<script>alert(1)</script>"),
                        "La empresa contiene caracteres no permitidos (solo letras, numeros, espacios y . , # / & ( ) ' ° -)"),
                caso("ocupacion con HTML", r -> r.setOcupacion("<img src=x onerror=alert(1)>"),
                        "La ocupacion contiene caracteres no permitidos (solo letras, numeros, espacios y . , # / & ( ) ' ° -)"),
                caso("calle con comillas dobles", r -> r.getDomicilio().setCalle("Hidalgo\" onmouseover=\"x"),
                        "La calle contiene caracteres no permitidos (solo letras, numeros, espacios y . , # / & ( ) ' ° -)"),
                // Antes provocaba StackOverflowError en el motor de regex (error 500)
                caso("nombre malicioso de 200 000 caracteres", r -> r.setNombre("a ".repeat(100_000) + "!"),
                        "El nombre solo admite letras y espacios"),
                caso("filtro de apellido malicioso", r -> r.setApellidoPaterno("b ".repeat(100_000)),
                        "El apellido paterno solo admite letras y espacios"),
                caso("nombre con espacio al inicio", r -> r.setNombre(" Jose"), "El nombre solo admite letras y espacios"),
                caso("segundo nombre invalido", r -> r.setSegundoNombre("Antonio2"), "El segundo nombre solo admite letras y espacios"),
                caso("apellido paterno obligatorio", r -> r.setApellidoPaterno(" "), "El apellido paterno es obligatorio"),
                caso("apellido materno con simbolos", r -> r.setApellidoMaterno("Martinez!"), "El apellido materno solo admite letras y espacios"),
                caso("fecha futura", r -> r.setFechaNacimiento(LocalDate.now().plusDays(1)), "La fecha de nacimiento no puede ser una fecha futura"),
                caso("menor de edad", r -> r.setFechaNacimiento(LocalDate.now().minusYears(18).plusDays(1)), "El cliente debe ser mayor de edad (18 anos o mas)"),
                caso("curp obligatoria", r -> r.setCurp(null), "La CURP es obligatoria"),
                caso("curp de 17 caracteres", r -> r.setCurp("BAMJ900512HGTLRS0"), "La CURP debe contener 18 caracteres"),
                caso("curp con formato invalido", r -> r.setCurp("BAMJ901312HGTLRS09"), "La CURP no tiene un formato valido"),
                caso("curp con sexo X", r -> r.setCurp("BAMJ900512XGTLRS09"), "La CURP no tiene un formato valido"),
                caso("rfc de 12 caracteres", r -> r.setRfc("BAM900512AB1"), "El RFC debe contener 13 caracteres"),
                caso("rfc con formato invalido", r -> r.setRfc("BAMJ9005A2AB1"), "El RFC no tiene un formato valido"),
                caso("sexo obligatorio", r -> r.setSexo(null), "El sexo es obligatorio"),
                caso("nacionalidad invalida", r -> r.setNacionalidad("Mexicana"), "La nacionalidad debe ser un codigo ISO de 3 letras, por ejemplo MEX"),
                caso("estado civil obligatorio", r -> r.setEstadoCivil(null), "El estado civil es obligatorio"),
                caso("correo obligatorio", r -> r.setCorreo(null), "El correo electronico es obligatorio"),
                caso("correo sin dominio", r -> r.setCorreo("jose@mail"), "El correo electronico no tiene un formato valido"),
                caso("correo de 101 caracteres", r -> r.setCorreo("a".repeat(92) + "@mail.com"), "El correo electronico admite maximo 100 caracteres"),
                caso("telefono de 9 digitos", r -> r.setTelefonoMovil("551234567"), "El telefono movil debe contener exactamente 10 digitos"),
                caso("telefono con letras", r -> r.setTelefonoMovil("55123456AB"), "El telefono movil debe contener exactamente 10 digitos"),
                caso("telefono alternativo invalido", r -> r.setTelefonoAlternativo("12345678901"), "El telefono alternativo debe contener exactamente 10 digitos"),
                caso("domicilio obligatorio", r -> r.setDomicilio(null), "El domicilio es obligatorio"),
                caso("codigo postal de 4 digitos", r -> r.getDomicilio().setCodigoPostal("3780"), "El codigo postal debe contener exactamente 5 digitos"),
                caso("calle obligatoria", r -> r.getDomicilio().setCalle(""), "La calle es obligatoria"),
                caso("colonia obligatoria", r -> r.getDomicilio().setColonia(null), "La colonia es obligatoria"),
                caso("ocupacion obligatoria", r -> r.setOcupacion(null), "La ocupacion es obligatoria"),
                caso("ingreso igual a cero", r -> r.setIngresoMensual(BigDecimal.ZERO), "El ingreso mensual debe ser mayor a cero"),
                caso("ingreso negativo", r -> r.setIngresoMensual(new BigDecimal("-1")), "El ingreso mensual debe ser mayor a cero"),
                caso("ingreso con 3 decimales", r -> r.setIngresoMensual(new BigDecimal("100.123")), "El ingreso mensual admite hasta 10 enteros y 2 decimales"),
                caso("contrasena obligatoria", r -> r.setPassword(null), "La contrasena es obligatoria"));
    }

    private static Arguments caso(String nombre, Consumer<ClienteRequest> modificacion, String mensaje) {
        return Arguments.of(nombre, modificacion, mensaje);
    }
}
