package com.example.hotel_management_system.services.payment;

import com.example.hotel_management_system.model.entities.Payment;

public interface PaymentGateway {

    PaymentGatewayResult process(Payment payment);
}
