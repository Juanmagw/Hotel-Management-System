package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.mappers.UserMapper;
import com.example.hotel_management_system.model.dtos.request.UserRequestDTO;
import com.example.hotel_management_system.model.dtos.response.UserResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper; // Inyectamos MapStruct

    // POST http://localhost:8080/api/users
    @PostMapping
    public ResponseEntity<UserResponseDTO> registerUser(@RequestBody UserRequestDTO request) {
        User userEntity = userMapper.toEntity(request);
        User savedUser = userService.registerUser(userEntity);
        UserResponseDTO response = userMapper.toResponseDTO(savedUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // GET http://localhost:8080/api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        // El servicio busca la Entity pura en la base de datos
        User userEntity = userService.getUserById(id);

        // Traducimos la Entity a un ResponseDTO seguro para que no viaje la contraseña por internet
        UserResponseDTO response = userMapper.toResponseDTO(userEntity);

        return ResponseEntity.ok(response);
    }
}