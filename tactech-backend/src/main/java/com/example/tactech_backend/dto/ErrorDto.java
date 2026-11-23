package com.example.tactech_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato unico de error para que la app siempre reciba la misma estructura. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto {
    private LocalDateTime fecha;
    private int estado;
    private String mensaje;
    /** Errores de validacion por campo (puede ir vacio). */
    private Map<String, String> errores;
}
