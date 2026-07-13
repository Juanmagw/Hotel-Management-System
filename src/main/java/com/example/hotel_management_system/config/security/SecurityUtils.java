package com.example.hotel_management_system.config.security;

import com.example.hotel_management_system.exceptions.AuthException;
import com.example.hotel_management_system.model.entities.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("Usuario no autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new AuthException("Sesión inválida");
        }

        return user;
    }
}
