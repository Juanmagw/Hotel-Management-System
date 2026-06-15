package com.example.hotel_management_system.mappers;

import com.example.hotel_management_system.model.dtos.response.ReservationResponseDTO;
import com.example.hotel_management_system.model.entities.Reservation;
import com.example.hotel_management_system.model.entities.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    // 1. Extrae el nombre desde el objeto anidado 'user.name' hacia 'userName'
    @Mapping(target = "userName", source = "user.name")
    // 2. Mapea la lista de Rooms hacia la lista de Strings (usará el método de abajo)
    @Mapping(target = "roomNumbers", source = "rooms")
    ReservationResponseDTO toResponseDTO(Reservation entity);

    // Metodo de apoyo: MapStruct lo detecta solo y lo aplica a cada elemento de la lista
    default String mapRoomToRoomNumber(Room room) {
        if (room == null) return null;
        return room.getRoomNumber();
    }
}
