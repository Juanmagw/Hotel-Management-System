package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.dtos.request.LoginRequestDTO;
import com.example.hotel_management_system.model.dtos.response.LoginResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder; // 👈 Mockeamos el encoder

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Debería iniciar sesión correctamente cuando las credenciales son válidas")
    void login_Success() {
        // GIVEN
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("password123")
                .build();

        User usuarioExistente = User.builder()
                .id(UUID.randomUUID())
                .name("Carlos")
                .email("carlos@email.com")
                .passwordHash("2a$10$ClaveHashEncriptadaDeEjemplo...") // Coincide
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioExistente));

        // Simulamos que el encoder dice "Sí, coinciden"
        when(passwordEncoder.matches("password123", usuarioExistente.getPasswordHash())).thenReturn(true);

        // WHEN
        LoginResponseDTO resultado = authService.login(request); // 👈 Cambiado a LoginResponseDTO

        // THEN
        assertThat(resultado, is(notNullValue()));
        assertThat(resultado.getToken(), startsWithIgnoringCase("fake-jwt-token-")); // Validamos que genere el token
        assertThat(resultado.getUserId(), is(usuarioExistente.getId()));
        assertThat(resultado.getEmail(), is("carlos@email.com"));
        assertThat(resultado.getName(), is("Carlos"));
        verify(userRepository, times(1)).findByEmail("carlos@email.com");
        verify(passwordEncoder, times(1)).matches("password123", usuarioExistente.getPasswordHash());
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException cuando la contraseña es incorrecta")
    void login_ThrowsException_WhenPasswordIsIncorrect() {
        // GIVEN
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("carlos@email.com")
                .password("clave_erronea") // No va a coincidir
                .build();

        User usuarioExistente = User.builder()
                .id(UUID.randomUUID())
                .email("carlos@email.com")
                .passwordHash("$2a$10$ClaveHashEncriptadaDeEjemplo...") // La real
                .build();

        when(userRepository.findByEmail("carlos@email.com")).thenReturn(Optional.of(usuarioExistente));

        // Simulamos que el encoder dice "No, no coinciden"
        when(passwordEncoder.matches("clave_erronea", usuarioExistente.getPasswordHash())).thenReturn(false);

        // WHEN & THEN
        assertThrows(ResourceNotValidException.class, () -> {
            authService.login(request);
        });
    }
}
