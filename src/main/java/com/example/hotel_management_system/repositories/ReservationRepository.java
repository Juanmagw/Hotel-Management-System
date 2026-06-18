package com.example.hotel_management_system.repositories;

import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.model.entities.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM Reservation r
            JOIN r.rooms room
            WHERE room.id IN :roomIds
              AND r.status NOT IN :excludedStatuses
              AND r.checkInDate < :checkOut
              AND r.checkOutDate > :checkIn
              AND (:excludeReservationId IS NULL OR r.id <> :excludeReservationId)
            """)
    boolean existsOverlappingReservation(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses,
            @Param("excludeReservationId") UUID excludeReservationId
    );
}
