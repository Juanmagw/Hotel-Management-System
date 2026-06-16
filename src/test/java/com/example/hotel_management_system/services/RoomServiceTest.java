package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.repositories.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("Debería retornar una lista con las habitaciones disponibles")
    void getAvailableRooms_Success() {
        // GIVEN
        Room room1 = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        Room room2 = Room.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).build();

        when(roomRepository.findByStatus(RoomStatus.AVAILABLE)).thenReturn(List.of(room1, room2));

        // WHEN
        List<Room> resultado = roomService.getAvailableRooms();

        // THEN
        assertThat(resultado, hasSize(2));
        assertThat(resultado.get(0).getRoomNumber(), is("101"));
        assertThat(resultado.get(1).getStatus(), is(RoomStatus.AVAILABLE));
        verify(roomRepository, times(1)).findByStatus(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Debería actualizar el estado de una habitación correctamente")
    void updateRoomStatus_Success() {
        // GIVEN
        Long roomId = 1L;
        Room habitacionOriginal = Room.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE) // Empieza disponible
                .build();

        Room habitacionActualizada = Room.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.OCCUPIED) // Cambia a ocupada
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(habitacionOriginal));
        when(roomRepository.save(ArgumentMatchers.any(Room.class))).thenReturn(habitacionActualizada);

        // WHEN
        Room resultado = roomService.updateRoomStatus(roomId, RoomStatus.OCCUPIED);

        // THEN
        assertThat(resultado, is(notNullValue()));
        assertThat(resultado.getStatus(), is(RoomStatus.OCCUPIED));
        verify(roomRepository, times(1)).findById(roomId);
        verify(roomRepository, times(1)).save(ArgumentMatchers.any(Room.class));
    }

    @Test
    @DisplayName("Debería lanzar RoomNotAvailableException cuando se intenta actualizar una habitación que no existe")
    void updateRoomStatus_ThrowsException_WhenRoomNotFound() {
        // GIVEN
        Long roomIdInexistente = 99L;

        // Simulamos que el repositorio devuelve un Optional vacío (no existe en BD)
        when(roomRepository.findById(roomIdInexistente)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(ResourceNotValidException.class, () -> {
            roomService.updateRoomStatus(roomIdInexistente, RoomStatus.OCCUPIED);
        });

        // Verificamos que jamás intente guardar nada si no la encontró
        verify(roomRepository, never()).save(ArgumentMatchers.any(Room.class));
    }
}
