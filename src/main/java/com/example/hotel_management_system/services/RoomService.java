package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.dtos.request.RoomRequestDTO;
import com.example.hotel_management_system.model.entities.ReservationStatus;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final Set<ReservationStatus> INACTIVE_RESERVATION_STATUSES = EnumSet.of(
            ReservationStatus.CANCELLED,
            ReservationStatus.CHECKED_OUT
    );

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public Page<Room> getAllRooms(Pageable pageable) {
        return roomRepository.findAll(pageable);
    }

    public Page<Room> getAvailableRooms(Pageable pageable) {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE, pageable);
    }

    public Page<Room> getAvailableRooms(Pageable pageable, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null && checkOut == null) {
            return getAvailableRooms(pageable);
        }

        if (checkIn == null || checkOut == null) {
            throw new ResourceNotValidException("Debe indicar checkIn y checkOut juntos.");
        }

        validateDateRange(checkIn, checkOut);

        return roomRepository.findAvailableForDateRange(
                RoomStatus.AVAILABLE,
                checkIn,
                checkOut,
                INACTIVE_RESERVATION_STATUSES,
                pageable
        );
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + id));
    }

    @Transactional
    public Room createRoom(RoomRequestDTO request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new ResourceAlreadyExistsException("Ya existe una habitación con el número: " + request.getRoomNumber());
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitación no encontrado con ID: " + request.getRoomTypeId()));

        RoomStatus status = request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE;

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomType(roomType)
                .status(status)
                .build();

        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long id, RoomRequestDTO request) {
        Room room = getRoomById(id);

        if (roomRepository.existsByRoomNumber(request.getRoomNumber())
                && !room.getRoomNumber().equals(request.getRoomNumber())) {
            throw new ResourceAlreadyExistsException("Ya existe una habitación con el número: " + request.getRoomNumber());
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitación no encontrado con ID: " + request.getRoomTypeId()));

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(roomType);
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = getRoomById(id);

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new ResourceNotValidException("No se puede eliminar la habitación " + room.getRoomNumber() + " porque no está disponible");
        }

        roomRepository.delete(room);
    }

    @Transactional
    public Room updateRoomStatus(Long roomId, RoomStatus newStatus) {
        Room room = getRoomById(roomId);
        room.setStatus(newStatus);
        return roomRepository.save(room);
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new ResourceNotValidException("La fecha de check-out debe ser posterior al check-in.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ResourceNotValidException("La fecha de check-in no puede ser anterior a hoy.");
        }
    }
}
