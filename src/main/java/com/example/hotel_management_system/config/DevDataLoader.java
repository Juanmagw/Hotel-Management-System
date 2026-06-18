package com.example.hotel_management_system.config;

import com.example.hotel_management_system.model.entities.Role;
import com.example.hotel_management_system.model.entities.Room;
import com.example.hotel_management_system.model.entities.RoomStatus;
import com.example.hotel_management_system.model.entities.RoomType;
import com.example.hotel_management_system.model.entities.User;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.RoomTypeRepository;
import com.example.hotel_management_system.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataLoader implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Datos demo ya presentes, omitiendo seed.");
            return;
        }

        log.info("Cargando datos demo de desarrollo...");

        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);

        userRepository.save(User.builder()
                .name("Administrador")
                .email("admin@hotel.com")
                .passwordHash(encodedPassword)
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .name("Recepción")
                .email("recepcion@hotel.com")
                .passwordHash(encodedPassword)
                .role(Role.RECEPTIONIST)
                .build());

        userRepository.save(User.builder()
                .name("Huésped Demo")
                .email("guest@hotel.com")
                .passwordHash(encodedPassword)
                .role(Role.GUEST)
                .build());

        RoomType standard = roomTypeRepository.save(RoomType.builder()
                .name("Standard")
                .capacity(2)
                .pricePerNight(new BigDecimal("80.00"))
                .build());

        RoomType deluxe = roomTypeRepository.save(RoomType.builder()
                .name("Deluxe")
                .capacity(3)
                .pricePerNight(new BigDecimal("120.00"))
                .build());

        RoomType suite = roomTypeRepository.save(RoomType.builder()
                .name("Suite")
                .capacity(4)
                .pricePerNight(new BigDecimal("200.00"))
                .build());

        saveRoom("101", standard);
        saveRoom("102", standard);
        saveRoom("103", standard);
        saveRoom("201", deluxe);
        saveRoom("202", deluxe);
        saveRoom("301", suite);

        log.info("Seed completado. Usuarios demo (contraseña: {}): admin@hotel.com, recepcion@hotel.com, guest@hotel.com",
                DEMO_PASSWORD);
    }

    private void saveRoom(String roomNumber, RoomType roomType) {
        roomRepository.save(Room.builder()
                .roomNumber(roomNumber)
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .build());
    }
}
