package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La fecha de nacimiento debe corresponder a una persona de al menos {@link #edadMinima()} anos.
 * Un valor nulo se considera valido; la obligatoriedad se valida con @NotNull.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MayorDeEdadValidator.class)
public @interface MayorDeEdad {

    String message() default "El cliente debe ser mayor de edad (18 anos o mas)";

    int edadMinima() default 18;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
