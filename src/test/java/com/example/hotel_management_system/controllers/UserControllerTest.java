package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.UserMapper;
import com.example.hotel_management_system.model.dtos.request.UpdateUserRoleRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UserRequestDTO;
import com.example.hotel_management_system.model.dtos.response.UserResponseDTO;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UserController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("POST /api/users -> Debería registrar un usuario y retornar 201 Created")
    void registerUser_ShouldReturnCreated_WhenDataIsValid() throws Exception {
        UserRequestDTO request = UserRequestDTO.builder()
                .name("Diego")
                .email("diego@email.com")
                .password("segura123")
                .build();

        UUID mockUserId = UUID.randomUUID();
        User mockUserEntity = new User();

        UserResponseDTO mockResponse = UserResponseDTO.builder()
                .id(mockUserId)
                .name("Diego")
                .email("diego@email.com")
                .role(Role.GUEST)
                .build();

        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(mockUserEntity);
        when(userService.registerUser(any(User.class))).thenReturn(mockUserEntity);
        when(userMapper.toResponseDTO(any(User.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(mockUserId.toString()))
                .andExpect(jsonPath("$.name").value("Diego"))
                .andExpect(jsonPath("$.email").value("diego@email.com"));
    }

    @Test
    @DisplayName("GET /api/users/me -> Debería retornar el perfil del usuario autenticado")
    void getCurrentUser_ShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        User currentUser = User.builder().id(userId).name("Ana").email("ana@test.com").role(Role.GUEST).build();
        UserResponseDTO response = UserResponseDTO.builder()
                .id(userId)
                .name("Ana")
                .email("ana@test.com")
                .role(Role.GUEST)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(userMapper.toResponseDTO(currentUser)).thenReturn(response);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Ana"));
    }

    @Test
    @DisplayName("GET /api/users -> Debería retornar página de usuarios")
    void getAllUsers_ShouldReturnPage() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Admin").email("admin@test.com").role(Role.ADMIN).build();
        UserResponseDTO dto = UserResponseDTO.builder().id(userId).name("Admin").email("admin@test.com").role(Role.ADMIN).build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);
        when(userMapper.toResponseDTO(user)).thenReturn(dto);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Admin"));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> Debería retornar 200 OK si el usuario existe")
    void getUserById_ShouldReturnOk_WhenUserExists() throws Exception {
        UUID userId = UUID.randomUUID();
        User requester = User.builder().id(userId).role(Role.GUEST).build();
        User target = User.builder().id(userId).name("Diego").email("diego@email.com").build();
        UserResponseDTO mockResponse = UserResponseDTO.builder()
                .id(userId)
                .name("Diego")
                .email("diego@email.com")
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(userService.getUserById(eq(userId), eq(requester))).thenReturn(target);
        when(userMapper.toResponseDTO(target)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Diego"));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> Debería retornar 404 si el usuario no existe")
    void getUserById_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        User requester = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

        when(securityUtils.getCurrentUser()).thenReturn(requester);
        when(userService.getUserById(eq(idInexistente), eq(requester)))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado con el ID: " + idInexistente));

        mockMvc.perform(get("/api/users/{id}", idInexistente))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/role -> Debería actualizar el rol y retornar 200 OK")
    void updateUserRole_ShouldReturnOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRoleRequestDTO request = UpdateUserRoleRequestDTO.builder().role(Role.RECEPTIONIST).build();
        User updated = User.builder().id(userId).name("Staff").role(Role.RECEPTIONIST).build();
        UserResponseDTO response = UserResponseDTO.builder().id(userId).name("Staff").role(Role.RECEPTIONIST).build();

        when(userService.updateUserRole(userId, Role.RECEPTIONIST)).thenReturn(updated);
        when(userMapper.toResponseDTO(updated)).thenReturn(response);

        mockMvc.perform(patch("/api/users/{id}/role", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"));
    }
}
