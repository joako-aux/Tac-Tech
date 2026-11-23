package com.example.tactech_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RutValidoValidator implements ConstraintValidator<RutValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        // null lo controla @NotBlank; aqui solo se revisa el formato y el digito
        return valor == null || Rut.esValido(valor);
    }
}
