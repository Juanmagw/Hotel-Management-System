package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.*;
import com.example.hotel_management_system.repositories.ReservationRepository;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public Reservation createReservation(UUID userId, List<Long> roomIds, java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        // 1. Validar usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se puede crear la reserva porque el usuario no existe."));

        // 2. Validar y obtener habitaciones
        List<Room> rooms = roomRepository.findAllById(roomIds);
        for (Long roomId : roomIds) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new ResourceNotFoundException("La habitación con ID " + roomId + " no existe."));

            if (room.getStatus() != RoomStatus.AVAILABLE) {
                throw new ResourceNotValidException("La habitación número " + room.getRoomNumber() + " no está disponible.");
            }
        }

        // Verificar que todas estén disponibles
        boolean allAvailable = rooms.stream().allMatch(room -> room.getStatus() == RoomStatus.AVAILABLE);
        if (!allAvailable) {
            throw new RuntimeException("Alguna de las habitaciones seleccionadas no está disponible");
        }

        // 3. Calcular noches de estadía
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new ResourceNotValidException("La fecha de check-in no puede ser posterior a la de check-out.");        }

        // 4. Calcular precio total (Suma de precios por noche de cada habitación * número de noches)
        BigDecimal totalPrice = rooms.stream()
                .map(room -> room.getRoomType().getPricePerNight())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(nights));

        // 5. Construir y guardar la reserva usando @Builder de Lombok
        Reservation reservation = Reservation.builder()
                .user(user)
                .rooms(rooms)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .build();

        // 6. Cambiar estado de las habitaciones a OCCUPIED para que nadie más las pille
        rooms.forEach(room -> room.setStatus(RoomStatus.OCCUPIED));
        roomRepository.saveAll(rooms);

        return reservationRepository.save(reservation);
    }
}
