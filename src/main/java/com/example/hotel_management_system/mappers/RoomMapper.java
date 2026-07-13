package com.example.hotel_management_system.mappers;

import com.example.hotel_management_system.model.dtos.response.RoomResponseDTO;
import com.example.hotel_management_system.model.entities.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "roomTypeId", source = "roomType.id")
    @Mapping(target = "roomTypeName", source = "roomType.name")
    @Mapping(target = "capacity", source = "roomType.capacity")
    @Mapping(target = "pricePerNight", source = "roomType.pricePerNight")
    RoomResponseDTO toResponseDTO(Room room);
}
