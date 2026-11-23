package com.example.tactech_backend.entity;

/** Mismos nombres que el enum de la app Android: viajan por JSON como texto. */
public enum EstadoTicket {
    ABIERTO, COORDINACION_EN_PROCESO, COORDINADO, TRANSITO, TRABAJO_EN_PROCESO, PENDIENTE_POR_CLIENTE, PENDIENTE_POR_TACTECH, CERRADO, CANCELADO;

    /** Cerrado y Cancelado son estados finales: el ticket queda terminado. */
    public boolean esFinal() {
        return this == CERRADO || this == CANCELADO;
    }
}
