package com.example.hotel_management_system.repositories;

import com.example.hotel_management_system.model.entities.ReservationStatus;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Page<Room> findByStatus(RoomStatus status, Pageable pageable);

    @Query("""
            SELECT rm FROM Room rm
            WHERE rm.status = :status
              AND NOT EXISTS (
                SELECT 1 FROM Reservation r JOIN r.rooms room
                WHERE room.id = rm.id
                  AND r.status NOT IN :excludedStatuses
                  AND r.checkInDate < :checkOut
                  AND r.checkOutDate > :checkIn
              )
            """)
    Page<Room> findAvailableForDateRange(
            @Param("status") RoomStatus status,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses,
            Pageable pageable
    );

    boolean existsByRoomNumber(String roomNumber);

    boolean existsByRoomTypeId(Long roomTypeId);
}
