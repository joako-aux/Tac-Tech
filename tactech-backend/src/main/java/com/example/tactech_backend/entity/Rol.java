package com.example.tactech_backend.entity;

public enum Rol {
    /** Crea y gestiona usuarios, empresas, solicitudes y tickets. */
    ADMINISTRADOR,
    /** Soporte tecnico en terreno: atiende los tickets que le asignan. */
    TECNICO,
    /** Usuario de una empresa cliente: crea solicitudes de servicio. */
    CLIENTE
}
