package com.playstop.backend.exception;

/**
 * Error de negocio esperado (ej. "Cancha no encontrada", "El email ya está
 * registrado"). Se distingue de un RuntimeException sin tipo, que
 * GlobalExceptionHandler trata como bug de programación (500).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
