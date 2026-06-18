package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.mappers.RoomTypeMapper;
import com.example.hotel_management_system.model.dtos.request.RoomTypeRequestDTO;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeMapper roomTypeMapper;

    public Page<RoomType> getAllRoomTypes(Pageable pageable) {
        return roomTypeRepository.findAll(pageable);
    }

    public RoomType getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitación no encontrado con ID: " + id));
    }

    @Transactional
    public RoomType createRoomType(RoomType roomType) {
        if (roomTypeRepository.findByName(roomType.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Ya existe un tipo de habitación con el nombre: " + roomType.getName());
        }
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public RoomType updateRoomType(Long id, RoomTypeRequestDTO request) {
        RoomType roomType = getRoomTypeById(id);

        if (roomTypeRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .isPresent()) {
            throw new ResourceAlreadyExistsException("Ya existe un tipo de habitación con el nombre: " + request.getName());
        }

        roomTypeMapper.updateEntity(request, roomType);
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void deleteRoomType(Long id) {
        RoomType roomType = getRoomTypeById(id);

        if (roomRepository.existsByRoomTypeId(id)) {
            throw new ResourceNotValidException("No se puede eliminar el tipo '" + roomType.getName() + "' porque tiene habitaciones asociadas");
        }

        roomTypeRepository.delete(roomType);
    }
}
