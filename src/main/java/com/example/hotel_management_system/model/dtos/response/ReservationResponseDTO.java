package com.example.hotel_management_system.model.dtos.response;

import com.example.hotel_management_system.model.entities.ReservationStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReservationResponseDTO {
    private UUID id;
    private String userName;       // Solo el nombre del cliente
    private List<String> roomNumbers; // Los números de las habitaciones reservadas (ej: ["101", "102"])
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private ReservationStatus status;
}
