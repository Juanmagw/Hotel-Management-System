package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.PaymentMapper;
import com.example.hotel_management_system.mappers.ReservationMapper;
import com.example.hotel_management_system.model.dtos.request.ReservationRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UpdateReservationStatusRequestDTO;
import com.example.hotel_management_system.model.dtos.response.PaymentResponseDTO;
import com.example.hotel_management_system.model.dtos.response.ReservationResponseDTO;
import com.example.hotel_management_system.model.entities.Payment;
import com.example.hotel_management_system.model.entities.PaymentStatus;
import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.model.entities.ReservationStatus;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.PaymentService;
import com.example.hotel_management_system.services.ReservationService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ReservationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private ReservationMapper reservationMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @MockitoBean
    private SecurityUtils securityUtils;

    private User guestUser() {
        return User.builder().id(UUID.randomUUID()).name("Carlos").role(Role.GUEST).build();
    }

    @Test
    @DisplayName("GET /api/reservations/{id}/payment -> Debería retornar pago de la reserva")
    void getPaymentByReservationId_ShouldReturnOk() throws Exception {
        User guest = guestUser();
        UUID reservationId = UUID.randomUUID();
        Payment payment = new Payment();
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .reservationId(reservationId)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(paymentService.getPaymentByReservationId(reservationId, guest)).thenReturn(payment);
        when(paymentMapper.toResponseDTO(payment)).thenReturn(dto);

        mockMvc.perform(get("/api/reservations/{id}/payment", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/reservations -> Debería retornar página de reservas")
    void getAllReservations_ShouldReturnPage() throws Exception {
        User guest = guestUser();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation();
        ReservationResponseDTO dto = ReservationResponseDTO.builder()
                .id(reservationId)
                .userName("Carlos")
                .status(ReservationStatus.PENDING)
                .build();
        Page<Reservation> page = new PageImpl<>(List.of(reservation));

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(reservationService.getAllReservations(eq(guest), any(Pageable.class))).thenReturn(page);
        when(reservationMapper.toResponseDTO(reservation)).thenReturn(dto);

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userName").value("Carlos"));
    }

    @Test
    @DisplayName("GET /api/reservations/{id} -> Debería retornar reserva por ID")
    void getReservationById_ShouldReturnOk() throws Exception {
        User guest = guestUser();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation();
        ReservationResponseDTO dto = ReservationResponseDTO.builder()
                .id(reservationId)
                .userName("Carlos")
                .roomNumbers(List.of("101"))
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(reservationService.getReservationById(reservationId, guest)).thenReturn(reservation);
        when(reservationMapper.toResponseDTO(reservation)).thenReturn(dto);

        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.roomNumbers[0]").value("101"));
    }

    @Test
    @DisplayName("POST /api/reservations -> Debería retornar 201 Created")
    void createReservation_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        User guest = guestUser();
        ReservationRequestDTO request = ReservationRequestDTO.builder()
                .userId(guest.getId())
                .roomIds(List.of(1L, 2L))
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .build();

        Reservation mockReservation = new Reservation();
        UUID mockReservationId = UUID.randomUUID();
        ReservationResponseDTO mockResponseDTO = ReservationResponseDTO.builder()
                .id(mockReservationId)
                .userName("Carlos")
                .roomNumbers(List.of("101", "102"))
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(new BigDecimal("300.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(reservationService.createReservation(
                eq(guest.getId()), eq(request.getRoomIds()),
                eq(request.getCheckInDate()), eq(request.getCheckOutDate()), eq(guest)))
                .thenReturn(mockReservation);
        when(reservationMapper.toResponseDTO(mockReservation)).thenReturn(mockResponseDTO);

        mockMvc.perform(post("/api/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(mockReservationId.toString()))
                .andExpect(jsonPath("$.userName").value("Carlos"))
                .andExpect(jsonPath("$.roomNumbers[0]").value("101"))
                .andExpect(jsonPath("$.totalPrice").value(300.00))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/reservations/{id}/status -> Debería actualizar estado")
    void updateReservationStatus_ShouldReturnOk() throws Exception {
        User receptionist = User.builder().id(UUID.randomUUID()).role(Role.RECEPTIONIST).build();
        UUID reservationId = UUID.randomUUID();
        UpdateReservationStatusRequestDTO request = UpdateReservationStatusRequestDTO.builder()
                .status(ReservationStatus.CONFIRMED)
                .build();
        Reservation updated = new Reservation();
        ReservationResponseDTO response = ReservationResponseDTO.builder()
                .id(reservationId)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(receptionist);
        when(reservationService.updateReservationStatus(reservationId, ReservationStatus.CONFIRMED, receptionist))
                .thenReturn(updated);
        when(reservationMapper.toResponseDTO(updated)).thenReturn(response);

        mockMvc.perform(patch("/api/reservations/{id}/status", reservationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("DELETE /api/reservations/{id} -> Debería cancelar y retornar 204")
    void cancelReservation_ShouldReturnNoContent() throws Exception {
        User guest = guestUser();
        UUID reservationId = UUID.randomUUID();

        when(securityUtils.getCurrentUser()).thenReturn(guest);

        mockMvc.perform(delete("/api/reservations/{id}", reservationId).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/reservations/{id} -> Debería retornar 404 si no existe")
    void getReservationById_ShouldReturnNotFound() throws Exception {
        User guest = guestUser();
        UUID reservationId = UUID.randomUUID();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(reservationService.getReservationById(reservationId, guest))
                .thenThrow(new ResourceNotFoundException("Reserva no encontrada"));

        mockMvc.perform(get("/api/reservations/{id}", reservationId))
                .andExpect(status().isNotFound());
    }
}
