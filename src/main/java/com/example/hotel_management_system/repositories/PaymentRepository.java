package com.example.hotel_management_system.repositories;

import com.example.hotel_management_system.model.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    // Busca el pago asociado a un ID de reserva
    Optional<Payment> findByReservationId(UUID reservationId);
}