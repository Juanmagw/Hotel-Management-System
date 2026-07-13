package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.dtos.request.RoomRequestDTO;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.RoomTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    @DisplayName("Debería retornar una página con las habitaciones disponibles")
    void getAvailableRooms_Success() {
        Room room1 = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        Room room2 = Room.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Room> page = new PageImpl<>(List.of(room1, room2));

        when(roomRepository.findByStatus(RoomStatus.AVAILABLE, pageable)).thenReturn(page);

        Page<Room> resultado = roomService.getAvailableRooms(pageable);

        assertThat(resultado.getContent(), hasSize(2));
        assertThat(resultado.getContent().get(0).getRoomNumber(), is("101"));
    }

    @Test
    @DisplayName("Debería retornar todas las habitaciones paginadas")
    void getAllRooms_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Room room = Room.builder().id(1L).roomNumber("101").build();
        Page<Room> page = new PageImpl<>(List.of(room));

        when(roomRepository.findAll(pageable)).thenReturn(page);

        Page<Room> result = roomService.getAllRooms(pageable);

        assertThat(result.getContent(), hasSize(1));
    }

    @Test
    @DisplayName("Debería crear una habitación")
    void createRoom_Success() {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        RoomRequestDTO request = RoomRequestDTO.builder().roomNumber("201").roomTypeId(1L).build();

        when(roomRepository.existsByRoomNumber("201")).thenReturn(false);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(suite));
        when(roomRepository.save(ArgumentMatchers.any(Room.class))).thenAnswer(inv -> {
            Room saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        Room result = roomService.createRoom(request);

        assertThat(result.getId(), is(5L));
        assertThat(result.getRoomNumber(), is("201"));
        assertThat(result.getStatus(), is(RoomStatus.AVAILABLE));
    }

    @Test
    @DisplayName("Debería rechazar número de habitación duplicado")
    void createRoom_ThrowsWhenRoomNumberExists() {
        RoomRequestDTO request = RoomRequestDTO.builder().roomNumber("101").roomTypeId(1L).build();
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("Debería actualizar el estado de una habitación correctamente")
    void updateRoomStatus_Success() {
        Long roomId = 1L;
        Room habitacionOriginal = Room.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(habitacionOriginal));
        when(roomRepository.save(ArgumentMatchers.any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room resultado = roomService.updateRoomStatus(roomId, RoomStatus.OCCUPIED);

        assertThat(resultado.getStatus(), is(RoomStatus.OCCUPIED));
    }

    @Test
    @DisplayName("Debería lanzar excepción al actualizar habitación inexistente")
    void updateRoomStatus_ThrowsException_WhenRoomNotFound() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                roomService.updateRoomStatus(99L, RoomStatus.OCCUPIED));
    }

    @Test
    @DisplayName("No debería eliminar habitación que no está disponible")
    void deleteRoom_ThrowsWhenNotAvailable() {
        Room room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.OCCUPIED).build();
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(ResourceNotValidException.class, () -> roomService.deleteRoom(1L));
        verify(roomRepository, never()).delete(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Debería eliminar habitación disponible")
    void deleteRoom_Success() {
        Room room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        roomService.deleteRoom(1L);

        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("Debería actualizar una habitación existente")
    void updateRoom_Success() {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        Room existing = Room.builder().id(1L).roomNumber("101").roomType(suite).status(RoomStatus.AVAILABLE).build();
        RoomRequestDTO request = RoomRequestDTO.builder()
                .roomNumber("101")
                .roomTypeId(1L)
                .status(RoomStatus.MAINTENANCE)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(suite));
        when(roomRepository.save(existing)).thenReturn(existing);

        Room result = roomService.updateRoom(1L, request);

        assertThat(result.getStatus(), is(RoomStatus.MAINTENANCE));
    }
}
