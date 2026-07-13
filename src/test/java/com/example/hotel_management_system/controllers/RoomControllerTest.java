package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.RoomMapper;
import com.example.hotel_management_system.model.dtos.request.RoomRequestDTO;
import com.example.hotel_management_system.model.dtos.response.RoomResponseDTO;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.services.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RoomController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private RoomMapper roomMapper;

    @Test
    @DisplayName("GET /api/rooms -> Debería retornar página de habitaciones")
    void getAllRooms_ShouldReturnPage() throws Exception {
        RoomType suite = RoomType.builder().id(1L).name("Suite").capacity(2).pricePerNight(new BigDecimal("100")).build();
        Room room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(suite).build();
        RoomResponseDTO dto = RoomResponseDTO.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        Page<Room> page = new PageImpl<>(List.of(room));

        when(roomService.getAllRooms(any(Pageable.class))).thenReturn(page);
        when(roomMapper.toResponseDTO(room)).thenReturn(dto);

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].roomNumber").value("101"));
    }

    @Test
    @DisplayName("GET /api/rooms/available -> Debería retornar 200 OK y página de habitaciones disponibles")
    void getAvailableRooms_ShouldReturnListAndOk() throws Exception {
        RoomType suite = RoomType.builder().id(1L).name("Suite").capacity(2).pricePerNight(new BigDecimal("100")).build();
        Room room1 = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(suite).build();
        Room room2 = Room.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(suite).build();

        RoomResponseDTO dto1 = RoomResponseDTO.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        RoomResponseDTO dto2 = RoomResponseDTO.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).build();

        Page<Room> roomPage = new PageImpl<>(List.of(room1, room2));
        when(roomService.getAvailableRooms(any(Pageable.class), isNull(), isNull())).thenReturn(roomPage);
        when(roomMapper.toResponseDTO(room1)).thenReturn(dto1);
        when(roomMapper.toResponseDTO(room2)).thenReturn(dto2);

        mockMvc.perform(get("/api/rooms/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].roomNumber").value("101"))
                .andExpect(jsonPath("$.content[1].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/rooms/{id} -> Debería retornar habitación por ID")
    void getRoomById_ShouldReturnOk() throws Exception {
        Room room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        RoomResponseDTO dto = RoomResponseDTO.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();

        when(roomService.getRoomById(1L)).thenReturn(room);
        when(roomMapper.toResponseDTO(room)).thenReturn(dto);

        mockMvc.perform(get("/api/rooms/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("101"));
    }

    @Test
    @DisplayName("POST /api/rooms -> Debería crear habitación y retornar 201")
    void createRoom_ShouldReturnCreated() throws Exception {
        RoomRequestDTO request = RoomRequestDTO.builder().roomNumber("301").roomTypeId(1L).build();
        Room created = Room.builder().id(3L).roomNumber("301").status(RoomStatus.AVAILABLE).build();
        RoomResponseDTO response = RoomResponseDTO.builder().id(3L).roomNumber("301").status(RoomStatus.AVAILABLE).build();

        when(roomService.createRoom(any(RoomRequestDTO.class))).thenReturn(created);
        when(roomMapper.toResponseDTO(created)).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("301"));
    }

    @Test
    @DisplayName("PUT /api/rooms/{id} -> Debería actualizar habitación")
    void updateRoom_ShouldReturnOk() throws Exception {
        RoomRequestDTO request = RoomRequestDTO.builder()
                .roomNumber("101")
                .roomTypeId(1L)
                .status(RoomStatus.MAINTENANCE)
                .build();
        Room updated = Room.builder().id(1L).roomNumber("101").status(RoomStatus.MAINTENANCE).build();
        RoomResponseDTO response = RoomResponseDTO.builder().id(1L).roomNumber("101").status(RoomStatus.MAINTENANCE).build();

        when(roomService.updateRoom(eq(1L), any(RoomRequestDTO.class))).thenReturn(updated);
        when(roomMapper.toResponseDTO(updated)).thenReturn(response);

        mockMvc.perform(put("/api/rooms/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("DELETE /api/rooms/{id} -> Debería retornar 204")
    void deleteRoom_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", 1L).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/rooms/{id}/status -> Debería actualizar el estado y retornar 200 OK")
    void updateRoomStatus_ShouldReturnUpdatedRoom_WhenIdExists() throws Exception {
        Long roomId = 1L;
        Room roomActualizada = Room.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.MAINTENANCE)
                .build();

        RoomResponseDTO responseDTO = RoomResponseDTO.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.MAINTENANCE)
                .build();

        when(roomService.updateRoomStatus(anyLong(), any(RoomStatus.class))).thenReturn(roomActualizada);
        when(roomMapper.toResponseDTO(roomActualizada)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/rooms/{id}/status", roomId)
                        .param("newStatus", "MAINTENANCE")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("PATCH /api/rooms/{id}/status -> Debería retornar 404 si la habitación no existe")
    void updateRoomStatus_ShouldReturnError_WhenRoomNotFound() throws Exception {
        when(roomService.updateRoomStatus(anyLong(), any(RoomStatus.class)))
                .thenThrow(new ResourceNotFoundException("Habitación no encontrada"));

        mockMvc.perform(patch("/api/rooms/{id}/status", 99L)
                        .param("newStatus", "OCCUPIED")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
