package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.mappers.RoomTypeMapper;
import com.example.hotel_management_system.model.dtos.request.RoomTypeRequestDTO;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.RoomTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomTypeServiceTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeMapper roomTypeMapper;

    @InjectMocks
    private RoomTypeService roomTypeService;

    @Test
    @DisplayName("Debería retornar página de tipos de habitación")
    void getAllRoomTypes_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        Page<RoomType> page = new PageImpl<>(List.of(suite));

        when(roomTypeRepository.findAll(pageable)).thenReturn(page);

        Page<RoomType> result = roomTypeService.getAllRoomTypes(pageable);

        assertThat(result.getContent(), hasSize(1));
        assertThat(result.getContent().get(0).getName(), is("Suite"));
    }

    @Test
    @DisplayName("Debería obtener tipo de habitación por ID")
    void getRoomTypeById_Success() {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(suite));

        RoomType result = roomTypeService.getRoomTypeById(1L);

        assertThat(result.getName(), is("Suite"));
    }

    @Test
    @DisplayName("Debería lanzar excepción si el tipo no existe")
    void getRoomTypeById_ThrowsWhenNotFound() {
        when(roomTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomTypeService.getRoomTypeById(99L));
    }

    @Test
    @DisplayName("Debería crear un tipo de habitación")
    void createRoomType_Success() {
        RoomType input = RoomType.builder().name("Deluxe").capacity(2).pricePerNight(new BigDecimal("150")).build();

        when(roomTypeRepository.findByName("Deluxe")).thenReturn(Optional.empty());
        when(roomTypeRepository.save(input)).thenAnswer(inv -> {
            RoomType saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        RoomType result = roomTypeService.createRoomType(input);

        assertThat(result.getId(), is(1L));
        verify(roomTypeRepository).save(input);
    }

    @Test
    @DisplayName("Debería rechazar nombre duplicado al crear")
    void createRoomType_ThrowsWhenNameExists() {
        RoomType input = RoomType.builder().name("Suite").build();
        when(roomTypeRepository.findByName("Suite")).thenReturn(Optional.of(new RoomType()));

        assertThrows(ResourceAlreadyExistsException.class, () -> roomTypeService.createRoomType(input));
        verify(roomTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería actualizar un tipo de habitación")
    void updateRoomType_Success() {
        RoomTypeRequestDTO request = RoomTypeRequestDTO.builder()
                .name("Suite Premium")
                .capacity(3)
                .pricePerNight(new BigDecimal("200"))
                .build();
        RoomType existing = RoomType.builder().id(1L).name("Suite").build();

        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomTypeRepository.findByName("Suite Premium")).thenReturn(Optional.empty());
        when(roomTypeRepository.save(existing)).thenReturn(existing);

        RoomType result = roomTypeService.updateRoomType(1L, request);

        assertThat(result, is(notNullValue()));
        verify(roomTypeMapper).updateEntity(request, existing);
    }

    @Test
    @DisplayName("No debería eliminar tipo con habitaciones asociadas")
    void deleteRoomType_ThrowsWhenRoomsExist() {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(suite));
        when(roomRepository.existsByRoomTypeId(1L)).thenReturn(true);

        assertThrows(ResourceNotValidException.class, () -> roomTypeService.deleteRoomType(1L));
        verify(roomTypeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debería eliminar tipo sin habitaciones asociadas")
    void deleteRoomType_Success() {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(suite));
        when(roomRepository.existsByRoomTypeId(1L)).thenReturn(false);

        roomTypeService.deleteRoomType(1L);

        verify(roomTypeRepository).delete(suite);
    }
}
