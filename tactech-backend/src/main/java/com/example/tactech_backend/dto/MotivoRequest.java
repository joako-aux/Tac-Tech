package com.example.tactech_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Motivo de cancelacion (cliente) o de rechazo (administrador). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotivoRequest {

    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
    private String motivo;
}
