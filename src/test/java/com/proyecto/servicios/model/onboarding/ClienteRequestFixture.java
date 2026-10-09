package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.Sexo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request de registro valido que cada prueba modifica segun el caso. */
public final class ClienteRequestFixture {

    public static ClienteRequest requestValido() {
        ClienteRequest request = new ClienteRequest();
        request.setNombre("Jose");
        request.setSegundoNombre("Antonio");
        request.setApellidoPaterno("Balderas");
        request.setApellidoMaterno("Martínez");
        request.setFechaNacimiento(LocalDate.of(1990, 5, 12));
        request.setCurp("BAMJ900512HGTLRS09");
        request.setRfc("BAMJ900512AB1");
        request.setSexo(Sexo.H);
        request.setNacionalidad("MEX");
        request.setEstadoCivil(EstadoCivil.SOLTERO);
        request.setCorreo("jose@mail.com");
        request.setTelefonoMovil("5512345678");
        request.setTelefonoAlternativo("5587654321");
        request.setOcupacion("Ingeniero");
        request.setEmpresa("ACME");
        request.setIngresoMensual(new BigDecimal("25000.00"));
        request.setPassword("Segura#2026");

        DomicilioRequest domicilio = new DomicilioRequest();
        domicilio.setCalle("Hidalgo");
        domicilio.setNumeroExterior("10");
        domicilio.setNumeroInterior("2B");
        domicilio.setColonia("Ampliacion 15 de Septiembre");
        domicilio.setCodigoPostal("37806");
        request.setDomicilio(domicilio);
        return request;
    }

    private ClienteRequestFixture() {
    }
}
