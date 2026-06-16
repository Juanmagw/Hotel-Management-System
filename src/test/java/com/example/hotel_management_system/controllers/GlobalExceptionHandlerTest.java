package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 🛠️ CONFIGURACIÓN STANDALONE: Enlazamos el controlador ficticio con tu Handler real manualmente
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler()) // 👈 Forzamos a que use tu Handler
                .build();
    }

    @Test
    @DisplayName("1. ResourceNotValidException -> Debería retornar 400 Bad Request")
    void handleResourceNotValid_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/test/bad-request")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Recurso Inválido"))
                .andExpect(jsonPath("$.message").value("El formato del recurso es incorrecto"))
                .andExpect(jsonPath("$.path").value("/test/bad-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("2. AuthException -> Debería retornar 401 Unauthorized")
    void handleAuth_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/test/unauthorized")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas de prueba"))
                .andExpect(jsonPath("$.path").value("/test/unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("3. ResourceNotFoundException -> Debería retornar 404 Not Found")
    void handleResourceNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/not-found")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso No Encontrado"))
                .andExpect(jsonPath("$.message").value("No se encontró el elemento buscado"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    @DisplayName("4. UserAlreadyExistsException -> Debería retornar 409 Conflict")
    void handleUserAlreadyExists_ShouldReturn409() throws Exception {
        mockMvc.perform(get("/test/conflict")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflicto de Datos"))
                .andExpect(jsonPath("$.message").value("El email ya se encuentra registrado"));
    }

    @Test
    @DisplayName("5. Exception Genérica -> Debería retornar 500 Internal Server Error")
    void handleGenericException_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/test/internal-error")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error Interno del Servidor"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error inesperado: Fallo crítico simulado"));
    }

    // --- CONTROLADOR FICTICIO (DUMMY) PARA PROPAGAR LAS EXCEPCIONES ---
    @RestController
    static class TestController {

        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new ResourceNotValidException("El formato del recurso es incorrecto");
        }

        @GetMapping("/test/unauthorized")
        public void throwUnauthorized() {
            throw new AuthException("Credenciales inválidas de prueba");
        }

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("No se encontró el elemento buscado");
        }

        @GetMapping("/test/conflict")
        public void throwConflict() {
            throw new ResourceAlreadyExistsException("El email ya se encuentra registrado");
        }

        @GetMapping("/test/internal-error")
        public void throwInternalError() throws Exception {
            throw new Exception("Fallo crítico simulado");
        }
    }
}
