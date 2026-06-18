package com.example.hotel_management_system.model.dtos.response;

import com.example.hotel_management_system.model.entities.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {

    private Long id;
    private String roomNumber;
    private RoomStatus status;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer capacity;
    private BigDecimal pricePerNight;
}
