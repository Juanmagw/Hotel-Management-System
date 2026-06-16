package com.example.hotel_management_system.model.dtos.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int status;       // Código HTTP (400, 404, etc.)
    private String error;     // Nombre del error (Bad Request, Not Found)
    private String message;   // Tu mensaje personalizado
    private String path;      // La URL que provocó el fallo
}
