package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.*;
import com.example.hotel_management_system.model.dtos.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice // Dice a Spring: "Escucha todos los controladores por si fallan"
public class GlobalExceptionHandler {

    // 1. HABITACIÓN NO DISPONIBLE -> HTTP 400
    @ExceptionHandler(ResourceNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotValid(ResourceNotValidException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Recurso Inválido", ex.getMessage(), request);
    }

    // 2. CREDENCIALES INCORRECTAS -> HTTP 401
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Object> handleAuth(AuthException ex, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value()); // 👈 Código 401
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED); // 👈 Retorna 401
    }

    // 3. RECURSO NO ENCONTRADO -> HTTP 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Recurso No Encontrado", ex.getMessage(), request);
    }

    // 4. USUARIO DUPLICADO -> HTTP 409 (Conflict es ideal para datos duplicados)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExists(ResourceAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflicto de Datos", ex.getMessage(), request);
    }

    // 5    . ERROR GENÉRICO NO CONTROLADO -> HTTP 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno del Servidor", "Ocurrió un error inesperado: " + ex.getMessage(), request);
    }

    // Metodo privado auxiliar para no repetir código escribiendo builders
    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(HttpStatus status, String errorType, String message, HttpServletRequest request) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorType)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(error, status);
    }
}
