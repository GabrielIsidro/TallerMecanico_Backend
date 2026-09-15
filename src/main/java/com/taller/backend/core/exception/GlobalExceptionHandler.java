package com.taller.backend.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Maneja nuestra validación de seguridad principal
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex.getMessage() != null) {
            String msg = ex.getMessage().toLowerCase();
            if (msg.contains("no encontrado") || msg.contains("no encontrada")) {
                status = HttpStatus.NOT_FOUND;
            } else if (msg.contains("acceso denegado") || msg.contains("sin permisos")) {
                status = HttpStatus.FORBIDDEN;
            } else if (msg.contains("ya existe") || msg.contains("ya está en uso") || msg.contains("registrado")) {
                status = HttpStatus.CONFLICT;
            } else if (msg.contains("inválido") || msg.contains("incorrecta")) {
                status = HttpStatus.BAD_REQUEST;
            } else {
                status = HttpStatus.BAD_REQUEST; // Por defecto tratamos las de lógica de negocio como 400
            }
        }

        return new ResponseEntity<>(response, status);
    }

    // Para cualquier otro error no capturado que reviente Java internamente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Error interno del servidor. Por favor, intente de nuevo más tarde.");
        // Imprimimos el error real en la consola del backend para debugging
        ex.printStackTrace();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
