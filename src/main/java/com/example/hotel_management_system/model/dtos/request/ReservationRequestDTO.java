package com.example.hotel_management_system.model.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private UUID userId;

    @NotEmpty(message = "Debe indicar al menos una habitación")
    private List<Long> roomIds;

    @NotNull(message = "La fecha de check-in es obligatoria")
    private LocalDate checkInDate;

    @NotNull(message = "La fecha de check-out es obligatoria")
    private LocalDate checkOutDate;
}
