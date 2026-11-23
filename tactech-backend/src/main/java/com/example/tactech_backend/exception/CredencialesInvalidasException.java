package com.example.tactech_backend.exception;

/** Correo o clave incorrectos. Se traduce a HTTP 401. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Correo o clave incorrectos");
    }
}
