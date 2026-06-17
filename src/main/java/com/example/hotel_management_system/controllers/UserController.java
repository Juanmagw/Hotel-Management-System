package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.mappers.UserMapper;
import com.example.hotel_management_system.model.dtos.request.UpdateUserRoleRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UserRequestDTO;
import com.example.hotel_management_system.model.dtos.response.UserResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestión de usuarios")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Registrar un nuevo usuario (rol GUEST)")
    @SecurityRequirements
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRequestDTO request) {
        User savedUser = userService.registerUser(userMapper.toEntity(request));
        return new ResponseEntity<>(userMapper.toResponseDTO(savedUser), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDTO> getCurrentUser() {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(userMapper.toResponseDTO(currentUser));
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios (solo ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UserResponseDTO> users = userService.getAllUsers(pageable)
                .map(userMapper::toResponseDTO);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID (propio perfil o staff)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        User requester = securityUtils.getCurrentUser();
        User user = userService.getUserById(id, requester);
        return ResponseEntity.ok(userMapper.toResponseDTO(user));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Actualizar el rol de un usuario (solo ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequestDTO request) {
        User updatedUser = userService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(userMapper.toResponseDTO(updatedUser));
    }
}
