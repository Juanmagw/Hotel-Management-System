package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.UserMapper;
import com.example.hotel_management_system.model.dtos.request.UserRequestDTO;
import com.example.hotel_management_system.model.dtos.response.UserResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UserController.class, GlobalExceptionHandler.class}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper; // 👈 Mockeamos también el Mapper de usuarios

    @Test
    @DisplayName("POST /api/users -> Debería registrar un usuario y retornar 201 Created")
    void registerUser_ShouldReturnCreated_WhenDataIsValid() throws Exception {
        // GIVEN
        UserRequestDTO request = UserRequestDTO.builder()
                .name("Diego")
                .email("diego@email.com")
                .password("segura123")
                .build();

        UUID mockUserId = UUID.randomUUID();
        User mockUserEntity = new User(); // Entidad intermedia simulada

        UserResponseDTO mockResponse = UserResponseDTO.builder()
                .id(mockUserId)
                .name("Diego")
                .email("diego@email.com")
                .role(null) // Si tienes roles específicos en tu Enum (ej: Role.CLIENT), puedes ponerlo aquí
                .build();

        // Entrenamos el flujo completo de simulación: Mapper -> Service -> Mapper
        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(mockUserEntity);
        when(userService.registerUser(any(User.class))).thenReturn(mockUserEntity);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mockUserId.toString()))
                .andExpect(jsonPath("$.name").value("Diego"))
                .andExpect(jsonPath("$.email").value("diego@email.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> Debería retornar 200 OK y el usuario si el UUID existe")
    void getUserById_ShouldReturnOk_WhenUserExists() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        User mockUserEntity = new User();

        UserResponseDTO mockResponse = UserResponseDTO.builder()
                .id(userId)
                .name("Diego")
                .email("diego@email.com")
                .build();

        when(userService.getUserById(userId)).thenReturn(mockUserEntity);
        when(userMapper.toResponseDTO(mockUserEntity)).thenReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Diego"));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> Debería retornar 404 Not Found si el usuario no existe")
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        // GIVEN
        UUID idInexistente = UUID.randomUUID();

        // Forzamos al servicio a lanzar la excepción de recurso no encontrado
        when(userService.getUserById(idInexistente))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con ID: " + idInexistente));

        // WHEN & THEN
        mockMvc.perform(get("/api/users/{id}", idInexistente)
                        .contentType(MediaType.APPLICATION_JSON))
                // Comprobamos que el GlobalExceptionHandler haga su magia traduciéndolo a un 404
                .andExpect(status().isNotFound());
    }
}
