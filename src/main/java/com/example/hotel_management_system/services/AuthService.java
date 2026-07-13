package com.example.hotel_management_system.services;

import com.example.hotel_management_system.config.security.JwtService;
import com.example.hotel_management_system.exceptions.AuthException;
import com.example.hotel_management_system.model.dtos.request.AuthRequestDTO;
import com.example.hotel_management_system.model.dtos.response.AuthResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDTO login(AuthRequestDTO request) {
        try {
            // 1. Spring Security intenta autenticar al usuario físicamente en la BD
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            // 2. Si las credenciales fallan, disparamos tu excepción personalizada
            throw new AuthException("El email o la contraseña son incorrectos");
        }

        // 3. Si va bien, recuperamos al usuario completo de la BD
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        // 4. Generamos su JWT correspondiente
        String jwtToken = jwtService.generateToken((UserDetails) user);

        // 5. Devolvemos el envoltorio con el token listo para el cliente
        return AuthResponseDTO.builder()
                .token(jwtToken)
                .build();
    }
}
