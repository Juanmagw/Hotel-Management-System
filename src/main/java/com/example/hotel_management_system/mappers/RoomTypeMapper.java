package com.example.hotel_management_system.mappers;

import com.example.hotel_management_system.model.dtos.request.RoomTypeRequestDTO;
import com.example.hotel_management_system.model.dtos.response.RoomTypeResponseDTO;
import com.example.hotel_management_system.model.entities.RoomType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomTypeMapper {

    @Mapping(target = "id", ignore = true)
    RoomType toEntity(RoomTypeRequestDTO dto);

    RoomTypeResponseDTO toResponseDTO(RoomType entity);

    @Mapping(target = "id", ignore = true)
    void updateEntity(RoomTypeRequestDTO dto, @MappingTarget RoomType entity);
}
