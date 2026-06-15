package com.example.hotel_management_system.model.dtos.request;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReservationRequestDTO {
    private UUID userId;
    private List<Long> roomIds;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
