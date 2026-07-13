package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.mappers.RoomMapper;
import com.example.hotel_management_system.model.dtos.request.RoomRequestDTO;
import com.example.hotel_management_system.model.dtos.response.RoomResponseDTO;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.services.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
@Tag(name = "Rooms", description = "Gestión de habitaciones")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @GetMapping
    @Operation(summary = "Listar todas las habitaciones (paginado, staff)")
    public ResponseEntity<Page<RoomResponseDTO>> getAllRooms(
            @PageableDefault(size = 10, sort = "roomNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoomResponseDTO> response = roomService.getAllRooms(pageable)
                .map(roomMapper::toResponseDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    @Operation(summary = "Listar habitaciones disponibles (paginado; opcionalmente por rango de fechas)")
    public ResponseEntity<Page<RoomResponseDTO>> getAvailableRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @PageableDefault(size = 10, sort = "roomNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoomResponseDTO> response = roomService.getAvailableRooms(pageable, checkIn, checkOut)
                .map(roomMapper::toResponseDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener habitación por ID (staff)")
    public ResponseEntity<RoomResponseDTO> getRoomById(@PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        return ResponseEntity.ok(roomMapper.toResponseDTO(room));
    }

    @PostMapping
    @Operation(summary = "Crear habitación (solo ADMIN)")
    public ResponseEntity<RoomResponseDTO> createRoom(@Valid @RequestBody RoomRequestDTO request) {
        Room created = roomService.createRoom(request);
        return new ResponseEntity<>(roomMapper.toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar habitación (solo ADMIN)")
    public ResponseEntity<RoomResponseDTO> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequestDTO request) {
        Room updated = roomService.updateRoom(id, request);
        return ResponseEntity.ok(roomMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar habitación (solo ADMIN)")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado de una habitación")
    public ResponseEntity<RoomResponseDTO> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam @NotNull(message = "El nuevo estado es obligatorio") RoomStatus newStatus) {
        Room updatedRoom = roomService.updateRoomStatus(id, newStatus);
        return ResponseEntity.ok(roomMapper.toResponseDTO(updatedRoom));
    }
}
