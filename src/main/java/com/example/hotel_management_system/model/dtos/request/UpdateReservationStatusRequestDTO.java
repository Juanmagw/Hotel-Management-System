package com.example.hotel_management_system.model.dtos.request;

import com.example.hotel_management_system.model.entities.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationStatusRequestDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private ReservationStatus status;
}
