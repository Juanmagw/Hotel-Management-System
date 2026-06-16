package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Crea el constructor automático para inyectar el repository
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("El correo electrónico " + user.getEmail() + " ya está registrado.");        }
        // Guardamos el usuario directamente en la base de datos
        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el ID: " + id));    }
}
