package com.example.tactech_backend.exception;

import com.example.tactech_backend.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte las excepciones en respuestas JSON con un codigo HTTP claro.
 * Los controladores quedan limpios: solo lanzan la excepcion.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid fallo: 400 con el mensaje de cada campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos invalidos", errores);
    }

    /** JSON mal formado o un enum con un valor que no existe. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> jsonInvalido(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido", Map.of());
    }

    /** Parametro con un valor que no corresponde, por ejemplo ?rol=JEFE. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Valor invalido para el parametro " + e.getName(), Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDto> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage(), Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorDto> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.CONFLICT, e.getMessage(), Map.of());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorDto> credenciales(CredencialesInvalidasException e) {
        return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage(), Map.of());
    }

    private ResponseEntity<ErrorDto> respuesta(HttpStatus estado, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(estado)
                .body(new ErrorDto(LocalDateTime.now(), estado.value(), mensaje, errores));
    }
}
