package com.example.hotel_management_system.mappers;

import com.example.hotel_management_system.model.dtos.response.PaymentResponseDTO;
import com.example.hotel_management_system.model.entities.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "reservationId", source = "reservation.id")
    PaymentResponseDTO toResponseDTO(Payment payment);
}
