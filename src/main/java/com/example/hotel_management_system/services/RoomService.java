package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.repositories.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    @Transactional
    public Room updateRoomStatus(Long roomId, RoomStatus newStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotValidException("Habitación no encontrada"));
        room.setStatus(newStatus);
        return roomRepository.save(room); // Al estar con @Transactional, Hibernate lo actualiza solo, pero save() es más explícito
    }
}
