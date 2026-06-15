package com.example.hotel_management_system.repositories;

import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.model.entities.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    // Busca todas las reservas que pertenecen a un usuario específico
    List<Reservation> findByUserId(UUID userId);

    // Filtra las reservas por su estado (PENDING, CONFIRMED, etc.)
    List<Reservation> findByStatus(ReservationStatus status);
}
