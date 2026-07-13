package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.mappers.PaymentMapper;
import com.example.hotel_management_system.model.dtos.request.PaymentRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UpdatePaymentStatusRequestDTO;
import com.example.hotel_management_system.model.dtos.response.PaymentResponseDTO;
import com.example.hotel_management_system.model.entities.Payment;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Gestión de pagos")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Registrar pago de una reserva (mock gateway para CARD/PAYPAL)")
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO request) {
        User requester = securityUtils.getCurrentUser();
        Payment payment = paymentService.createPayment(
                request.getReservationId(),
                request.getPaymentMethod(),
                requester
        );
        return new ResponseEntity<>(paymentMapper.toResponseDTO(payment), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pago por ID (dueño de la reserva o staff)")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(@PathVariable UUID id) {
        User requester = securityUtils.getCurrentUser();
        Payment payment = paymentService.getPaymentById(id, requester);
        return ResponseEntity.ok(paymentMapper.toResponseDTO(payment));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado del pago (staff; confirmar efectivo, reembolsos)")
    public ResponseEntity<PaymentResponseDTO> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentStatusRequestDTO request) {
        User requester = securityUtils.getCurrentUser();
        Payment updated = paymentService.updatePaymentStatus(id, request.getStatus(), requester);
        return ResponseEntity.ok(paymentMapper.toResponseDTO(updated));
    }
}
