package com.example.hotel_management_system.mappers;

import com.example.hotel_management_system.model.dtos.request.UserRequestDTO;
import com.example.hotel_management_system.model.dtos.response.UserResponseDTO;
import com.example.hotel_management_system.model.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Le decimos que mapee "password" del DTO hacia "passwordHash" de la Entity
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "id", ignore = true) // El ID lo genera la DB, lo ignoramos al crear
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRequestDTO dto);

    // Los campos que se llaman igual (name, email, role) se mapean solos automáticamente
    UserResponseDTO toResponseDTO(User entity);
}
