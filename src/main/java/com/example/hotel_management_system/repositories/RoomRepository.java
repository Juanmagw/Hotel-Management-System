package com.example.hotel_management_system.repositories;

import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    // Spring genera: SELECT * FROM rooms WHERE status = ?
    List<Room> findByStatus(RoomStatus status);
}
