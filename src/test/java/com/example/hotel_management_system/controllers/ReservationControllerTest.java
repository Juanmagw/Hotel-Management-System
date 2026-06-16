package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.mappers.ReservationMapper;
import com.example.hotel_management_system.model.dtos.request.ReservationRequestDTO;
import com.example.hotel_management_system.model.dtos.response.ReservationResponseDTO;
import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.services.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReservationController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private ReservationMapper reservationMapper;

    @Test
    @DisplayName("POST /api/reservations -> Debería retornar 201 Created y el DTO con UUID y nombres de habitación")
    void createReservation_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        ReservationRequestDTO request = ReservationRequestDTO.builder()
                .userId(userId)
                .roomIds(List.of(1L, 2L))
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .build();

        Reservation mockReservation = new Reservation();

        // Generamos un UUID real para el ID de la reserva ficticia
        UUID mockReservationId = UUID.randomUUID();

        // Construimos el Response DTO con tus campos exactos
        ReservationResponseDTO mockResponseDTO = ReservationResponseDTO.builder()
                .id(mockReservationId) // 👈 Ajustado a UUID
                .userName("Carlos")
                .roomNumbers(List.of("101", "102")) // 👈 Campos nuevos agregados
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(new BigDecimal("300.00"))
                .build();

        when(reservationService.createReservation(any(), any(), any(), any())).thenReturn(mockReservation);
        when(reservationMapper.toResponseDTO(any(Reservation.class))).thenReturn(mockResponseDTO);

        // WHEN & THEN
        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mockReservationId.toString())) // 👈 Validamos el UUID como String en el JSON
                .andExpect(jsonPath("$.userName").value("Carlos"))
                .andExpect(jsonPath("$.roomNumbers[0]").value("101"))
                .andExpect(jsonPath("$.roomNumbers[1]").value("102"))
                .andExpect(jsonPath("$.totalPrice").value(300.00));
    }
}
