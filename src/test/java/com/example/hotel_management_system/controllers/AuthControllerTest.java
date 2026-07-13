package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.AuthException;
import com.example.hotel_management_system.model.dtos.request.AuthRequestDTO;
import com.example.hotel_management_system.model.dtos.response.AuthResponseDTO;
import com.example.hotel_management_system.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class) // 1. Usamos la extensión de Mockito pura, sin cargar Spring
public class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService; // Mock del servicio de negocio

    @InjectMocks
    private AuthController authController; // Inyectamos el mock en el controlador real

    @BeforeEach
    void setUp() {
        // 2. CONFIGURACIÓN STANDALONE: Construimos el entorno web a mano inyectando tu Handler de errores
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login -> Debería retornar 200 OK y el Token JWT si las credenciales son válidas")
    void login_ShouldReturnToken_WhenCredentialsAreValid() throws Exception {
        // GIVEN
        AuthRequestDTO request = AuthRequestDTO.builder()
                .email("admin@hotel.com")
                .password("password123")
                .build();

        AuthResponseDTO mockResponse = AuthResponseDTO.builder()
                .token("eyJhbGciOiJIUzI1NiJ9.mockTokenLines.12345678")
                .build();

        when(authService.login(any(AuthRequestDTO.class))).thenReturn(mockResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.mockTokenLines.12345678"))
                .andExpect(jsonPath("$.type").value("Bearer")); // Valida que el campo final estático venga por defecto
    }

    @Test
    @DisplayName("POST /api/auth/login -> Debería retornar 401 Unauthorized si el login falla")
    void login_ShouldReturn401_WhenCredentialsAreInvalid() throws Exception {
        // GIVEN
        AuthRequestDTO requestWrong = AuthRequestDTO.builder()
                .email("hacker@hotel.com")
                .password("wrongpassword")
                .build();

        // Forzamos al servicio a lanzar tu excepción de credenciales incorrectas
        when(authService.login(any(AuthRequestDTO.class)))
                .thenThrow(new AuthException("El email o la contraseña son incorrectos"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWrong)))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("El email o la contraseña son incorrectos"));
    }
}