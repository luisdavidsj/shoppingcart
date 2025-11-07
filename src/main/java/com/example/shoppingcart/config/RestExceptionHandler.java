package com.example.shoppingcart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    private Map<String, Object> body(HttpStatus status, String error, Object detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("timestamp", OffsetDateTime.now());
        m.put("status", status.value());
        m.put("error", error);
        m.put("detail", detail);
        return m;
    }

    /**
     * Errores de validación (@Valid) en request bodies / params.
     * Devuelve un mapa { campo -> mensaje } con HTTP 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(body(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors));
    }

    /**
     * Peticiones inválidas controladas por la app (e.g., item no encontrado,
     * argumentos fuera de rango, etc.).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(body(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage()));
    }

    /**
     * Conflictos por concurrencia (optimistic locking).
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<?> handleConflict(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, "Concurrent modification", null));
    }

    /**
     * Errores que ya vienen con un status (p.ej. ResponseStatusException o derivados).
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<?> handleErrorResponse(ErrorResponseException ex) {
        log.warn("ErrorResponseException", ex);
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(body(status, "Error", ex.getBody()));
    }

    /**
     * Catch-all para cualquier excepción no manejada.
     * No expone el stacktrace al cliente, pero lo deja en logs.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex.getClass().getSimpleName()));
    }
}
