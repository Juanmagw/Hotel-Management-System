package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

// Importamos los Matchers de Hamcrest para que las comprobaciones se lean como inglés nativo
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class) // Activa el superpoder de Mockito en este test
public class UserServiceTest {

    @Mock
    private UserRepository userRepository; // Creamos un clon "falso" del repositorio

    @InjectMocks
    private UserService userService; // Mockito inyectará el repositorio falso dentro de este servicio

    @Test
    @DisplayName("Debería registrar un usuario exitosamente cuando el email no existe")
    void registerUser_Success() {
        // 1. PREPARACIÓN (GIVEN): Creamos los datos simulados
        User userEntrada = User.builder()
                .name("Alejandro")
                .email("ale@test.com")
                .passwordHash("encoded_pass")
                .role(Role.GUEST)
                .build();

        User userGuardadoEnDB = User.builder()
                .id(UUID.randomUUID()) // Simulamos que la DB le asigna un ID
                .name("Alejandro")
                .email("ale@test.com")
                .passwordHash("encoded_pass")
                .role(Role.GUEST)
                .build();

        // Configuramos el comportamiento del Mock (Stubbing)
        // "Cuando el servicio pregunte si existe el email, responde falso"
        lenient().when(userRepository.findByEmail("ale@test.com")).thenReturn(java.util.Optional.empty());
        // "Cuando el servicio intente guardar al usuario, devuélvele el objeto con ID"
        when(userRepository.save(userEntrada)).thenReturn(userGuardadoEnDB);

        // 2. EJECUCIÓN (WHEN): Llamamos al metodo real del servicio que queremos auditar
        User resultado = userService.registerUser(userEntrada);

        // 3. VERIFICACIÓN (THEN): Usamos Hamcrest para evaluar la calidad del resultado
        assertThat(resultado, is(notNullValue()));
        assertThat(resultado.getId(), is(notNullValue()));
        assertThat(resultado.getName(), is(equalTo("Alejandro")));
        assertThat(resultado.getEmail(), is(equalTo("ale@test.com")));

        // Verificación extra: Aseguramos que el repositorio real fue llamado exactamente 1 vez
        verify(userRepository, times(1)).save(userEntrada);
    }

    @Test
    @DisplayName("Debería lanzar una excepción cuando el email ya está registrado")
    void registerUser_ThrowsException_WhenEmailExists() {
        // GIVEN
        User userDuplicado = User.builder()
                .email("duplicado@test.com")
                .name("Juan")
                .build();

        // Configuramos el Mock: "Esta vez di que el correo SÍ existe"
        when(userRepository.findByEmail("duplicado@test.com")).thenReturn(java.util.Optional.of(userDuplicado));

        // WHEN & THEN: Evaluamos que se lance nuestra excepción personalizada ante el conflicto
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            userService.registerUser(userDuplicado);
        });

        // Verificamos que por seguridad, el metodo .save() NUNCA llegó a ejecutarse
        verify(userRepository, never()).save(ArgumentMatchers.any(User.class));
    }
}
