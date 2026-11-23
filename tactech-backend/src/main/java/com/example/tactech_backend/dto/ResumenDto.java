package com.example.tactech_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Totales para el panel del administrador. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDto {
    private long total;
    private long pendientes;
    private long terminados;
    private Map<String, Long> porEstado;
}
