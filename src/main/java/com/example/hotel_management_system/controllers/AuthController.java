package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.model.dtos.request.LoginRequestDTO;
import com.example.hotel_management_system.model.dtos.response.LoginResponseDTO;
import com.example.hotel_management_system.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}
