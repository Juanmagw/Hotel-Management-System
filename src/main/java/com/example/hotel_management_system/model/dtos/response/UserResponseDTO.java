package com.example.hotel_management_system.model.dtos.response;

import com.example.hotel_management_system.model.entities.Role;
import lombok.*;
import java.util.UUID;

// Lo que respondemos al exterior (¡Sin la contraseña!)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private Role role;
}