package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.*;
import com.example.hotel_management_system.repositories.ReservationRepository;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Set<ReservationStatus> INACTIVE_STATUSES = EnumSet.of(
            ReservationStatus.CANCELLED,
            ReservationStatus.CHECKED_OUT
    );

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED_TRANSITIONS = Map.of(
            ReservationStatus.PENDING, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CANCELLED),
            ReservationStatus.CONFIRMED, Set.of(ReservationStatus.CHECKED_IN, ReservationStatus.CANCELLED),
            ReservationStatus.CHECKED_IN, Set.of(ReservationStatus.CHECKED_OUT),
            ReservationStatus.CHECKED_OUT, Set.of(),
            ReservationStatus.CANCELLED, Set.of()
    );

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public Page<Reservation> getAllReservations(User requester, Pageable pageable) {
        if (isStaff(requester)) {
            return reservationRepository.findAll(pageable);
        }
        return reservationRepository.findByUserId(requester.getId(), pageable);
    }

    public Reservation getReservationById(UUID id, User requester) {
        Reservation reservation = getReservationOrThrow(id);
        assertCanViewReservation(requester, reservation);
        return reservation;
    }

    @Transactional
    public Reservation createReservation(
            UUID userId,
            List<Long> roomIds,
            LocalDate checkIn,
            LocalDate checkOut,
            User requester) {

        if (roomIds == null || roomIds.isEmpty()) {
            throw new ResourceNotValidException("Debe seleccionar al menos una habitación.");
        }

        assertCanCreateForUser(requester, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No se puede crear la reserva porque el usuario no existe."));

        validateDates(checkIn, checkOut);

        List<Room> rooms = roomIds.stream()
                .map(roomId -> roomRepository.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("La habitación con ID " + roomId + " no existe.")))
                .toList();

        validateRoomsForBooking(rooms, checkIn, checkOut, null);

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal totalPrice = rooms.stream()
                .map(room -> room.getRoomType().getPricePerNight())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(nights));

        Reservation reservation = Reservation.builder()
                .user(user)
                .rooms(rooms)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation updateReservationStatus(UUID id, ReservationStatus newStatus, User requester) {
        Reservation reservation = getReservationOrThrow(id);
        assertCanManageStatus(requester, reservation, newStatus);
        validateStatusTransition(reservation.getStatus(), newStatus);

        reservation.setStatus(newStatus);
        applyRoomStatusForReservation(reservation, newStatus);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(UUID id, User requester) {
        Reservation reservation = getReservationOrThrow(id);
        assertCanCancel(requester, reservation);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResourceNotValidException("La reserva ya está cancelada.");
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new ResourceNotValidException("No se puede cancelar una reserva con estado " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        releaseRooms(reservation);
        reservationRepository.save(reservation);
    }

    private Reservation getReservationOrThrow(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new ResourceNotValidException("La fecha de check-in no puede ser posterior o igual a la de check-out.");
        }
    }

    private void validateRoomsForBooking(
            List<Room> rooms,
            LocalDate checkIn,
            LocalDate checkOut,
            UUID excludeReservationId) {

        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                throw new ResourceNotValidException(
                        "La habitación número " + room.getRoomNumber() + " está en mantenimiento.");
            }
        }

        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        if (reservationRepository.existsOverlappingReservation(
                roomIds, checkIn, checkOut, INACTIVE_STATUSES, excludeReservationId)) {
            throw new ResourceNotValidException(
                    "Una o más habitaciones ya tienen una reserva activa en las fechas seleccionadas.");
        }
    }

    private void validateStatusTransition(ReservationStatus current, ReservationStatus next) {
        Set<ReservationStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ResourceNotValidException(
                    "No se puede cambiar el estado de " + current + " a " + next);
        }
    }

    private void applyRoomStatusForReservation(Reservation reservation, ReservationStatus newStatus) {
        switch (newStatus) {
            case CHECKED_IN -> occupyRooms(reservation);
            case CHECKED_OUT, CANCELLED -> releaseRooms(reservation);
            default -> { }
        }
    }

    private void occupyRooms(Reservation reservation) {
        reservation.getRooms().forEach(room -> room.setStatus(RoomStatus.OCCUPIED));
        roomRepository.saveAll(reservation.getRooms());
    }

    private void releaseRooms(Reservation reservation) {
        reservation.getRooms().forEach(room -> room.setStatus(RoomStatus.AVAILABLE));
        roomRepository.saveAll(reservation.getRooms());
    }

    private boolean isStaff(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.RECEPTIONIST;
    }

    private void assertCanViewReservation(User requester, Reservation reservation) {
        if (isStaff(requester)) {
            return;
        }
        if (!requester.getId().equals(reservation.getUser().getId())) {
            throw new AccessDeniedException("No tienes permiso para consultar esta reserva");
        }
    }

    private void assertCanCreateForUser(User requester, UUID targetUserId) {
        if (isStaff(requester)) {
            return;
        }
        if (!requester.getId().equals(targetUserId)) {
            throw new AccessDeniedException("No puedes crear reservas para otro usuario");
        }
    }

    private void assertCanCancel(User requester, Reservation reservation) {
        if (isStaff(requester)) {
            return;
        }
        if (!requester.getId().equals(reservation.getUser().getId())) {
            throw new AccessDeniedException("No tienes permiso para cancelar esta reserva");
        }
    }

    private void assertCanManageStatus(User requester, Reservation reservation, ReservationStatus newStatus) {
        if (newStatus == ReservationStatus.CANCELLED) {
            assertCanCancel(requester, reservation);
            return;
        }

        if (!isStaff(requester)) {
            throw new AccessDeniedException("Solo el personal del hotel puede cambiar el estado de la reserva");
        }
    }
}
