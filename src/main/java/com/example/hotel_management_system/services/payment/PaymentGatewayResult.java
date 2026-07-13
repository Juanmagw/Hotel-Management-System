package com.example.hotel_management_system.services.payment;

import com.example.hotel_management_system.model.entities.PaymentStatus;
import lombok.Builder;

@Builder
public record PaymentGatewayResult(
        PaymentStatus status,
        String message
) {
}
