package com.example.tactech_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Anotacion de validacion propia: el campo debe ser un RUT chileno con digito verificador correcto. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RutValidoValidator.class)
public @interface RutValido {
    String message() default "El RUT no es valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
