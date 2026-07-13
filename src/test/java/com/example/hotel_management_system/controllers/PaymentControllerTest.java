package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.mappers.PaymentMapper;
import com.example.hotel_management_system.model.dtos.request.PaymentRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UpdatePaymentStatusRequestDTO;
import com.example.hotel_management_system.model.dtos.response.PaymentResponseDTO;
import com.example.hotel_management_system.model.entities.Payment;
import com.example.hotel_management_system.model.entities.PaymentMethod;
import com.example.hotel_management_system.model.entities.PaymentStatus;
import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PaymentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @MockitoBean
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("POST /api/payments -> Debería crear pago y retornar 201")
    void createPayment_ShouldReturnCreated() throws Exception {
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        UUID reservationId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .reservationId(reservationId)
                .paymentMethod(PaymentMethod.CARD)
                .build();
        Payment payment = new Payment();
        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(paymentId)
                .reservationId(reservationId)
                .amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.CARD)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(paymentService.createPayment(reservationId, PaymentMethod.CARD, guest)).thenReturn(payment);
        when(paymentMapper.toResponseDTO(payment)).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    @DisplayName("GET /api/payments/{id} -> Debería retornar pago por ID")
    void getPaymentById_ShouldReturnOk() throws Exception {
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(paymentId)
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(paymentService.getPaymentById(paymentId, guest)).thenReturn(payment);
        when(paymentMapper.toResponseDTO(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));
    }

    @Test
    @DisplayName("PATCH /api/payments/{id}/status -> Debería actualizar estado del pago")
    void updatePaymentStatus_ShouldReturnOk() throws Exception {
        User receptionist = User.builder().id(UUID.randomUUID()).role(Role.RECEPTIONIST).build();
        UUID paymentId = UUID.randomUUID();
        UpdatePaymentStatusRequestDTO request = UpdatePaymentStatusRequestDTO.builder()
                .status(PaymentStatus.COMPLETED)
                .build();
        Payment updated = new Payment();
        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(paymentId)
                .status(PaymentStatus.COMPLETED)
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(receptionist);
        when(paymentService.updatePaymentStatus(paymentId, PaymentStatus.COMPLETED, receptionist))
                .thenReturn(updated);
        when(paymentMapper.toResponseDTO(updated)).thenReturn(response);

        mockMvc.perform(patch("/api/payments/{id}/status", paymentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/payments/{id} -> Debería retornar 404 si no existe")
    void getPaymentById_ShouldReturnNotFound() throws Exception {
        User guest = User.builder().role(Role.GUEST).build();
        UUID paymentId = UUID.randomUUID();

        when(securityUtils.getCurrentUser()).thenReturn(guest);
        when(paymentService.getPaymentById(eq(paymentId), eq(guest)))
                .thenThrow(new ResourceNotFoundException("Pago no encontrado"));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isNotFound());
    }
}
