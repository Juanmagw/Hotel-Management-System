package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.RoomTypeMapper;
import com.example.hotel_management_system.model.dtos.request.RoomTypeRequestDTO;
import com.example.hotel_management_system.model.dtos.response.RoomTypeResponseDTO;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.services.RoomTypeService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RoomTypeController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class RoomTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomTypeService roomTypeService;

    @MockitoBean
    private RoomTypeMapper roomTypeMapper;

    @Test
    @DisplayName("GET /api/room-types -> Debería retornar página de tipos")
    void getAllRoomTypes_ShouldReturnPage() throws Exception {
        RoomType suite = RoomType.builder().id(1L).name("Suite").capacity(2).pricePerNight(new BigDecimal("100")).build();
        RoomTypeResponseDTO dto = RoomTypeResponseDTO.builder().id(1L).name("Suite").capacity(2).pricePerNight(new BigDecimal("100")).build();
        Page<RoomType> page = new PageImpl<>(List.of(suite));

        when(roomTypeService.getAllRoomTypes(any(Pageable.class))).thenReturn(page);
        when(roomTypeMapper.toResponseDTO(suite)).thenReturn(dto);

        mockMvc.perform(get("/api/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Suite"));
    }

    @Test
    @DisplayName("GET /api/room-types/{id} -> Debería retornar tipo por ID")
    void getRoomTypeById_ShouldReturnOk() throws Exception {
        RoomType suite = RoomType.builder().id(1L).name("Suite").build();
        RoomTypeResponseDTO dto = RoomTypeResponseDTO.builder().id(1L).name("Suite").build();

        when(roomTypeService.getRoomTypeById(1L)).thenReturn(suite);
        when(roomTypeMapper.toResponseDTO(suite)).thenReturn(dto);

        mockMvc.perform(get("/api/room-types/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Suite"));
    }

    @Test
    @DisplayName("POST /api/room-types -> Debería crear tipo y retornar 201")
    void createRoomType_ShouldReturnCreated() throws Exception {
        RoomTypeRequestDTO request = RoomTypeRequestDTO.builder()
                .name("Deluxe")
                .capacity(2)
                .pricePerNight(new BigDecimal("120"))
                .build();
        RoomType entity = RoomType.builder().name("Deluxe").build();
        RoomType created = RoomType.builder().id(2L).name("Deluxe").capacity(2).pricePerNight(new BigDecimal("120")).build();
        RoomTypeResponseDTO response = RoomTypeResponseDTO.builder().id(2L).name("Deluxe").build();

        when(roomTypeMapper.toEntity(any(RoomTypeRequestDTO.class))).thenReturn(entity);
        when(roomTypeService.createRoomType(entity)).thenReturn(created);
        when(roomTypeMapper.toResponseDTO(created)).thenReturn(response);

        mockMvc.perform(post("/api/room-types")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Deluxe"));
    }

    @Test
    @DisplayName("PUT /api/room-types/{id} -> Debería actualizar tipo")
    void updateRoomType_ShouldReturnOk() throws Exception {
        RoomTypeRequestDTO request = RoomTypeRequestDTO.builder()
                .name("Suite Premium")
                .capacity(3)
                .pricePerNight(new BigDecimal("200"))
                .build();
        RoomType updated = RoomType.builder().id(1L).name("Suite Premium").build();
        RoomTypeResponseDTO response = RoomTypeResponseDTO.builder().id(1L).name("Suite Premium").build();

        when(roomTypeService.updateRoomType(eq(1L), any(RoomTypeRequestDTO.class))).thenReturn(updated);
        when(roomTypeMapper.toResponseDTO(updated)).thenReturn(response);

        mockMvc.perform(put("/api/room-types/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Suite Premium"));
    }

    @Test
    @DisplayName("DELETE /api/room-types/{id} -> Debería retornar 204")
    void deleteRoomType_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/room-types/{id}", 1L).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/room-types/{id} -> Debería retornar 404 si no existe")
    void getRoomTypeById_ShouldReturnNotFound() throws Exception {
        when(roomTypeService.getRoomTypeById(99L))
                .thenThrow(new ResourceNotFoundException("Tipo de habitación no encontrado"));

        mockMvc.perform(get("/api/room-types/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
