package com.example.hotel_management_system.model.dtos.request;

import com.example.hotel_management_system.model.entities.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "El ID de la reserva es obligatorio")
    private UUID reservationId;

    @NotNull(message = "El método de pago es obligatorio")
    private PaymentMethod paymentMethod;
}
