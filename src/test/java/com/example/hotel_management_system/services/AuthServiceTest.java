package com.example.hotel_management_system.services;


import com.example.hotel_management_system.config.security.JwtService;
import com.example.hotel_management_system.exceptions.AuthException;
import com.example.hotel_management_system.model.dtos.request.AuthRequestDTO;
import com.example.hotel_management_system.model.dtos.response.AuthResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService; // Inyecta automáticamente los 3 mocks de arriba aquí dentro

    @Test
    @DisplayName("Login Exitoso -> Debería retornar AuthResponseDTO con el token JWT")
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // GIVEN
        AuthRequestDTO request = new AuthRequestDTO("diego@email.com", "password123");
        User mockUser = new User(); // Asume que tu entidad User tiene sus setters o builder
        mockUser.setEmail("diego@email.com");
        String mockJwt = "eyJhbGciOiJIUzI1NiJ9.tokenSimulado.123";

        // Simulamos que la autenticación pasa sin lanzar excepciones
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));

        // Simulamos la búsqueda en la base de datos
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));

        // Simulamos la generación del token
        when(jwtService.generateToken((UserDetails) mockUser)).thenReturn(mockJwt);

        // WHEN
        AuthResponseDTO response = authService.login(request);

        // THEN
        assertNotNull(response);
        assertEquals(mockJwt, response.getToken());
        assertEquals("Bearer", response.getType());

        // Verificaciones de comportamiento: Nos aseguramos de que se llamó a cada metodo una vez
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, times(1)).generateToken((UserDetails) mockUser);
    }

    @Test
    @DisplayName("Login Fallido -> Debería lanzar InvalidCredentialsException si las credenciales son erróneas")
    void login_ShouldThrowInvalidCredentialsException_WhenAuthenticationFails() {
        // GIVEN
        AuthRequestDTO request = new AuthRequestDTO("incorrecto@email.com", "wrongpass");

        // Forzamos al AuthenticationManager a lanzar una excepción típica de Spring Security
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(request);
        });

        assertEquals("El email o la contraseña son incorrectos", exception.getMessage());

        // Aseguramos que el flujo se corta inmediatamente: NUNCA se debe buscar en BD ni generar token
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken((org.springframework.security.core.userdetails.UserDetails) any(User.class));
    }

    @Test
    @DisplayName("Login Fallido (Caso borde) -> Debería lanzar excepción si autentica pero el usuario desapareció de la BD")
    void login_ShouldThrowException_WhenUserNotFoundInDbAfterAuthentication() {
        // GIVEN
        AuthRequestDTO request = new AuthRequestDTO("fantasma@email.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));

        // El mánager dice que sí, pero la BD devuelve vacío (un borrado concurrente, por ejemplo)
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // WHEN & THEN
        AuthException exception = assertThrows(AuthException.class, () -> {
            authService.login(request);
        });

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(jwtService, never()).generateToken((UserDetails) any(User.class));
    }
}