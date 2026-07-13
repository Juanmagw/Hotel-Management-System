package com.example.hotel_management_system.model.dtos.request;

import com.example.hotel_management_system.model.entities.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequestDTO {

    @NotBlank(message = "El número de habitación es obligatorio")
    private String roomNumber;

    @NotNull(message = "El tipo de habitación es obligatorio")
    private Long roomTypeId;

    private RoomStatus status;
}
