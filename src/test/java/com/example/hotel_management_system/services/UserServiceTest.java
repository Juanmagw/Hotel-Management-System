package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Debería registrar un usuario exitosamente cuando el email no existe")
    void registerUser_Success() {
        User userEntrada = User.builder()
                .name("Alejandro")
                .email("ale@test.com")
                .passwordHash("plain_pass")
                .build();

        when(userRepository.findByEmail("ale@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain_pass")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        User resultado = userService.registerUser(userEntrada);

        assertThat(resultado, is(notNullValue()));
        assertThat(resultado.getRole(), is(Role.GUEST));
        assertThat(resultado.getPasswordHash(), is(equalTo("encoded_pass")));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole(), is(Role.GUEST));
    }

    @Test
    @DisplayName("Debería lanzar una excepción cuando el email ya está registrado")
    void registerUser_ThrowsException_WhenEmailExists() {
        User userDuplicado = User.builder()
                .email("duplicado@test.com")
                .name("Juan")
                .build();

        when(userRepository.findByEmail("duplicado@test.com")).thenReturn(Optional.of(userDuplicado));

        assertThrows(ResourceAlreadyExistsException.class, () -> userService.registerUser(userDuplicado));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debería permitir a un GUEST ver su propio perfil")
    void getUserById_AllowsGuestToViewSelf() {
        UUID userId = UUID.randomUUID();
        User requester = User.builder().id(userId).role(Role.GUEST).build();
        User target = User.builder().id(userId).name("Self").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));

        User result = userService.getUserById(userId, requester);

        assertThat(result.getName(), is("Self"));
    }

    @Test
    @DisplayName("Debería denegar acceso a un GUEST que consulta otro usuario")
    void getUserById_DeniesGuestViewingOtherUser() {
        UUID requesterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User requester = User.builder().id(requesterId).role(Role.GUEST).build();
        User target = User.builder().id(targetId).name("Other").build();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class, () -> userService.getUserById(targetId, requester));
    }

    @Test
    @DisplayName("Debería permitir a ADMIN consultar cualquier usuario")
    void getUserById_AllowsAdminToViewAnyUser() {
        UUID targetId = UUID.randomUUID();
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        User target = User.builder().id(targetId).name("Guest").build();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        User result = userService.getUserById(targetId, admin);

        assertThat(result.getName(), is("Guest"));
    }

    @Test
    @DisplayName("Debería retornar página de usuarios")
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().id(UUID.randomUUID()).name("Test").build();
        Page<User> page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(pageable);

        assertThat(result.getContent(), hasSize(1));
    }

    @Test
    @DisplayName("Debería actualizar el rol de un usuario")
    void updateUserRole_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).role(Role.GUEST).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUserRole(userId, Role.RECEPTIONIST);

        assertThat(result.getRole(), is(Role.RECEPTIONIST));
    }

    @Test
    @DisplayName("Debería lanzar excepción al actualizar rol de usuario inexistente")
    void updateUserRole_ThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserRole(userId, Role.ADMIN));
    }
}
