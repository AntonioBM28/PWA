package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MayorDeEdadValidator implements ConstraintValidator<MayorDeEdad, LocalDate> {

    private int edadMinima;

    @Override
    public void initialize(MayorDeEdad anotacion) {
        edadMinima = anotacion.edadMinima();
    }

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        return fechaNacimiento == null || !fechaNacimiento.plusYears(edadMinima).isAfter(LocalDate.now());
    }
}
