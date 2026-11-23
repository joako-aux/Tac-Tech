package com.example.tactech_backend.entity;

/** Ciclo de vida de una solicitud creada por un cliente. */
public enum EstadoSolicitud {
    /** Toda solicitud nace aqui. Es el unico estado en que el cliente puede cancelarla. */
    POR_CONFIRMAR,
    /** El administrador creo el ticket formal y lo asigno a un tecnico. */
    CONFIRMADA,
    /** El administrador la rechazo (con motivo). */
    RECHAZADA,
    /** El cliente la anulo antes de ser procesada. */
    CANCELADA
}
