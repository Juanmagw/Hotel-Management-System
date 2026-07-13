package com.example.hotel_management_system.controllers;

import com.example.hotel_management_system.mappers.RoomTypeMapper;
import com.example.hotel_management_system.model.dtos.request.RoomTypeRequestDTO;
import com.example.hotel_management_system.model.dtos.response.RoomTypeResponseDTO;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.services.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@Tag(name = "Room Types", description = "Gestión de tipos de habitación")
@SecurityRequirement(name = "bearerAuth")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final RoomTypeMapper roomTypeMapper;

    @GetMapping
    @Operation(summary = "Listar tipos de habitación (paginado)")
    public ResponseEntity<Page<RoomTypeResponseDTO>> getAllRoomTypes(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<RoomTypeResponseDTO> response = roomTypeService.getAllRoomTypes(pageable)
                .map(roomTypeMapper::toResponseDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tipo de habitación por ID")
    public ResponseEntity<RoomTypeResponseDTO> getRoomTypeById(@PathVariable Long id) {
        RoomType roomType = roomTypeService.getRoomTypeById(id);
        return ResponseEntity.ok(roomTypeMapper.toResponseDTO(roomType));
    }

    @PostMapping
    @Operation(summary = "Crear tipo de habitación (solo ADMIN)")
    public ResponseEntity<RoomTypeResponseDTO> createRoomType(@Valid @RequestBody RoomTypeRequestDTO request) {
        RoomType created = roomTypeService.createRoomType(roomTypeMapper.toEntity(request));
        return new ResponseEntity<>(roomTypeMapper.toResponseDTO(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de habitación (solo ADMIN)")
    public ResponseEntity<RoomTypeResponseDTO> updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequestDTO request) {
        RoomType updated = roomTypeService.updateRoomType(id, request);
        return ResponseEntity.ok(roomTypeMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tipo de habitación (solo ADMIN)")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.noContent().build();
    }
}
