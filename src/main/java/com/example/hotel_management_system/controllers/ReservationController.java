package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.mappers.ReservationMapper;
import com.example.hotel_management_system.model.dtos.request.ReservationRequestDTO;
import com.example.hotel_management_system.model.dtos.response.ReservationResponseDTO;
import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.services.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper; // Inyectamos la interfaz de MapStruct

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequestDTO request) {
        try {
            // 1. El servicio procesa usando los parámetros sueltos del RequestDTO
            Reservation reservation = reservationService.createReservation(
                    request.getUserId(),
                    request.getRoomIds(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );

            // 2. MapStruct se encarga de aplanar la compleja entidad en el ResponseDTO definitivo
            ReservationResponseDTO response = reservationMapper.toResponseDTO(reservation);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            // Captura cualquier validación que falle en el servicio (ej: habitación ocupada)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}