package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException; // O la que uses para 404
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.dtos.request.LoginRequestDTO;
import com.example.hotel_management_system.model.dtos.response.LoginResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // 1. Buscamos al usuario por email
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el email: " + loginRequest.getEmail()));

        // 2. Comparamos el texto plano que llega del DTO con el Hash encriptado de la DB
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotValidException("El email o la contraseña son incorrectos");
        }

        // 3. Generamos un token simulado para no romper el flujo del frontend
        String fakeToken = "fake-jwt-token-" + UUID.randomUUID();

        // 4. Devolvemos la respuesta estructurada
        return LoginResponseDTO.builder()
                .token(fakeToken)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
