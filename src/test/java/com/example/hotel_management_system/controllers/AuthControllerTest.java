package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.dtos.request.LoginRequestDTO;
import com.example.hotel_management_system.model.dtos.response.LoginResponseDTO;
import com.example.hotel_management_system.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 👈 ¡Nuevo Import correcto!import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // 👈 Nuestro simulador de peticiones HTTP

    @Autowired
    private ObjectMapper objectMapper; // 👈 Convierte objetos Java a JSON strings

    @MockitoBean
    private AuthService authService; // 👈 En controladores usamos @MockBean en lugar de @Mock

    @Test
    @DisplayName("POST /api/auth/login -> Debería retornar 200 OK y el token cuando las credenciales son válidas")
    void login_ShouldReturnOk_WhenCredentialsAreValid() throws Exception {
        // GIVEN
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("password123")
                .build();

        LoginResponseDTO mockResponse = LoginResponseDTO.builder()
                .token("fake-jwt-token-12345")
                .userId(UUID.randomUUID())
                .email("carlos@email.com")
                .name("Carlos")
                .build();

        // Entrenamos al mock del servicio
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockResponse);

        // WHEN & THEN (Simulamos la petición HTTP real)
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON) // Enviamos JSON
                        .content(objectMapper.writeValueAsString(request))) // Cuerpo de la petición
                .andExpect(status().isOk()) // Esperamos un HTTP 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("fake-jwt-token-12345")) // Validamos campos del JSON de salida
                .andExpect(jsonPath("$.name").value("Carlos"))
                .andExpect(jsonPath("$.email").value("carlos@email.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login -> Debería retornar 401 Unauthorized cuando las credenciales son inválidas")
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        // GIVEN
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("error@email.com")
                .password("clave_incorrecta")
                .build();

        // Simulamos que el servicio lanza tu excepción personalizada
        when(authService.login(any(LoginRequestDTO.class)))
                .thenThrow(new ResourceNotValidException("El email o la contraseña son incorrectos"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // ⚠️ NOTA: Si no tienes un GlobalExceptionHandler configurado para mapear esta excepción a 401,
                // Spring por defecto podría retornar un 500 (Internal Server Error).
                // Si te falla aquí devolviendo 500, pon provisionalmente status().isInternalServerError() para verificar.
                .andExpect(status().isUnauthorized());
    }
}
