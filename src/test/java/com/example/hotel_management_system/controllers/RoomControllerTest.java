package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.services.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RoomController.class, GlobalExceptionHandler.class}, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    @DisplayName("GET /api/rooms/available -> Debería retornar 200 OK y la lista de habitaciones disponibles")
    void getAvailableRooms_ShouldReturnListAndOk() throws Exception {
        // GIVEN
        Room room1 = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        Room room2 = Room.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).build();

        when(roomService.getAvailableRooms()).thenReturn(List.of(room1, room2));

        // WHEN & THEN
        mockMvc.perform(get("/api/rooms/available")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Valida que vengan 2 elementos en la raíz del JSON
                .andExpect(jsonPath("$[0].roomNumber").value("101"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("PATCH /api/rooms/{id}/status -> Debería actualizar el estado y retornar 200 OK")
    void updateRoomStatus_ShouldReturnUpdatedRoom_WhenIdExists() throws Exception {
        // GIVEN
        Long roomId = 1L;
        Room roomActualizada = Room.builder()
                .id(roomId)
                .roomNumber("101")
                .status(RoomStatus.MAINTENANCE) // Estado nuevo simulado
                .build();

        when(roomService.updateRoomStatus(anyLong(), any(RoomStatus.class))).thenReturn(roomActualizada);

        // WHEN & THEN
        mockMvc.perform(patch("/api/rooms/{id}/status", roomId)
                        .param("newStatus", "MAINTENANCE") // 👈 Inyectamos el @RequestParam
                        .with(csrf()) // Saltamos protección CSRF en el PATCH
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @DisplayName("PATCH /api/rooms/{id}/status -> Debería retornar Error si la habitación no existe")
    void updateRoomStatus_ShouldReturnError_WhenRoomNotFound() throws Exception {
        // GIVEN
        Long idInexistente = 99L;

        when(roomService.updateRoomStatus(anyLong(), any(RoomStatus.class)))
                .thenThrow(new ResourceNotFoundException("Habitación no encontrada"));

        // WHEN & THEN
        mockMvc.perform(patch("/api/rooms/{id}/status", idInexistente)
                        .param("newStatus", "OCCUPIED")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                // ⚠️ NOTA: Si en tu GlobalExceptionHandler manejas RoomNotAvailableException como un 404,
                // cambia esto a status().isNotFound(). Si lo mapeas como un 400, déjalo como isBadRequest().
                .andExpect(status().isNotFound());
    }
}
