package com.example.hotel_management_system.services.payment;

import com.example.hotel_management_system.model.entities.Payment;
import com.example.hotel_management_system.model.entities.PaymentMethod;
import com.example.hotel_management_system.model.entities.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult process(Payment payment) {
        if (payment.getPaymentMethod() == PaymentMethod.CASH) {
            return PaymentGatewayResult.builder()
                    .status(PaymentStatus.PENDING)
                    .message("Pago en efectivo pendiente de confirmación en recepción")
                    .build();
        }

        return PaymentGatewayResult.builder()
                .status(PaymentStatus.COMPLETED)
                .message("Pago simulado procesado correctamente")
                .build();
    }
}
