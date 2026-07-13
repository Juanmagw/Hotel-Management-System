package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.config.security.SecurityUtils;
import com.example.hotel_management_system.mappers.PaymentMapper;
import com.example.hotel_management_system.mappers.ReservationMapper;
import com.example.hotel_management_system.model.dtos.request.ReservationRequestDTO;
import com.example.hotel_management_system.model.dtos.request.UpdateReservationStatusRequestDTO;
import com.example.hotel_management_system.model.dtos.response.PaymentResponseDTO;
import com.example.hotel_management_system.model.dtos.response.ReservationResponseDTO;
import com.example.hotel_management_system.model.entities.Payment;
import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.services.PaymentService;
import com.example.hotel_management_system.services.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Gestión de reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Listar reservas (staff: todas; huésped: solo las propias)")
    public ResponseEntity<Page<ReservationResponseDTO>> getAllReservations(
            @PageableDefault(size = 10, sort = "checkInDate", direction = Sort.Direction.DESC) Pageable pageable) {
        User requester = securityUtils.getCurrentUser();
        Page<ReservationResponseDTO> response = reservationService.getAllReservations(requester, pageable)
                .map(reservationMapper::toResponseDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reserva por ID (dueño o staff)")
    public ResponseEntity<ReservationResponseDTO> getReservationById(@PathVariable UUID id) {
        User requester = securityUtils.getCurrentUser();
        Reservation reservation = reservationService.getReservationById(id, requester);
        return ResponseEntity.ok(reservationMapper.toResponseDTO(reservation));
    }

    @GetMapping("/{id}/payment")
    @Operation(summary = "Obtener el pago asociado a una reserva (dueño o staff)")
    public ResponseEntity<PaymentResponseDTO> getPaymentByReservationId(@PathVariable UUID id) {
        User requester = securityUtils.getCurrentUser();
        Payment payment = paymentService.getPaymentByReservationId(id, requester);
        return ResponseEntity.ok(paymentMapper.toResponseDTO(payment));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva reserva")
    public ResponseEntity<ReservationResponseDTO> createReservation(@Valid @RequestBody ReservationRequestDTO request) {
        User requester = securityUtils.getCurrentUser();
        Reservation reservation = reservationService.createReservation(
                request.getUserId(),
                request.getRoomIds(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                requester
        );
        return new ResponseEntity<>(reservationMapper.toResponseDTO(reservation), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar estado de reserva (staff; cancelación también vía DELETE)")
    public ResponseEntity<ReservationResponseDTO> updateReservationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReservationStatusRequestDTO request) {
        User requester = securityUtils.getCurrentUser();
        Reservation updated = reservationService.updateReservationStatus(id, request.getStatus(), requester);
        return ResponseEntity.ok(reservationMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar reserva (dueño o staff)")
    public ResponseEntity<Void> cancelReservation(@PathVariable UUID id) {
        User requester = securityUtils.getCurrentUser();
        reservationService.cancelReservation(id, requester);
        return ResponseEntity.noContent().build();
    }
}
