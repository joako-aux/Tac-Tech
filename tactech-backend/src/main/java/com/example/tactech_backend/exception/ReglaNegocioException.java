package com.example.tactech_backend.exception;

/** Una regla del negocio impide la operacion (ej: cerrar un ticket sin guias). Se traduce a HTTP 409. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
