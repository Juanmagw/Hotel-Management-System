package com.example.hotel_management_system.model.dtos.request;

import com.example.hotel_management_system.model.entities.Role;
import lombok.*;

// Lo que el cliente envía para registrarse
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserRequestDTO {
    private String name;
    private String email;
    private String password; // Contraseña limpia que viene del formulario
    private Role role;
}