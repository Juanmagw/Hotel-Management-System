package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("El correo electrónico " + user.getEmail() + " ya está registrado.");
        }

        user.setRole(Role.GUEST);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el ID: " + id));
    }

    public User getUserById(UUID id, User requester) {
        User user = getUserById(id);
        assertCanViewUser(requester, user);
        return user;
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User updateUserRole(UUID userId, Role newRole) {
        User user = getUserById(userId);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    private void assertCanViewUser(User requester, User target) {
        if (requester.getRole() == Role.ADMIN || requester.getRole() == Role.RECEPTIONIST) {
            return;
        }

        if (!requester.getId().equals(target.getId())) {
            throw new AccessDeniedException("No tienes permiso para consultar este usuario");
        }
    }
}
