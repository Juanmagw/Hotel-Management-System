package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.*;
import com.example.hotel_management_system.repositories.ReservationRepository;
import com.example.hotel_management_system.repositories.RoomRepository;
import com.example.hotel_management_system.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("Debería crear una reserva correctamente calculando el precio por las noches de estadía")
    void createReservation_Success() {
        // 1. GIVEN (Preparación)
        User cliente = User.builder().id(UUID.randomUUID()).name("Carlos").build();
        List<Long> roomIds = List.of(1L); // ID de la habitación es 1L
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(3); // 3 noches

        // Creamos el tipo de habitación con su precio (Ajusta 'pricePerNight' si tu RoomType usa otro nombre)
        RoomType tipoSuite = RoomType.builder()
                .id(1L)
                .name("Suite")
                .pricePerNight(new BigDecimal("100.00"))
                .build();

        // Creamos la habitación asociándole su tipo
        Room habitacion = Room.builder()
                .id(1L)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .roomType(tipoSuite) // 👈 Vinculamos el tipo aquí
                .build();

        Reservation reservaGuardada = Reservation.builder()
                .id(UUID.randomUUID())
                .user(cliente)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .totalPrice(new BigDecimal("300.00")) // 3 noches * 100.00
                .status(ReservationStatus.CONFIRMED)
                .rooms(List.of(habitacion))
                .build();

        // ENTRENAMOS LOS MOCKS: Simulamos lo que el servicio buscará en la DB
        when(userRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        // Soportamos tanto si usas findAllById() como si buscas en bucle con findById()
        when(roomRepository.findById(1L)).thenReturn(Optional.of(habitacion));

        // Entrenamos los mocks
        when(reservationRepository.save(ArgumentMatchers.any(Reservation.class))).thenReturn(reservaGuardada);

        // 2. WHEN (Ejecución)
        Reservation resultado = reservationService.createReservation(cliente.getId(),roomIds,checkIn, checkOut);

        // 3. THEN (Verificaciones)
        assertThat(resultado, is(notNullValue()));
        assertThat(resultado.getTotalPrice(), is(comparesEqualTo(new BigDecimal("300.00"))));
        //assertThat(habitacion.getStatus(), is(RoomStatus.OCCUPIED)); // El servicio debería cambiarla a OCCUPIED

        verify(reservationRepository, times(1)).save(ArgumentMatchers.any(Reservation.class));
    }

    @Test
    @DisplayName("Debería lanzar RoomNotAvailableException cuando una habitación de la lista ya está ocupada")
    void createReservation_ThrowsException_WhenRoomIsOccupied() {
        // GIVEN
        User cliente = User.builder().id(UUID.randomUUID()).name("Carlos").build();
        RoomType tipoStandard = RoomType.builder().id(2L).pricePerNight(new BigDecimal("80.00")).build();
        List<Long> roomIds = List.of(2L); // ID de la habitación es 2L
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(2);

        Room habitacionOcupada = Room.builder()
                .id(2L)
                .roomNumber("102")
                .status(RoomStatus.OCCUPIED) // ⚠️ YA ESTÁ OCUPADA
                .roomType(tipoStandard)
                .build();

        // Entrenamos los mocks con findById para la habitación 2L
        when(userRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(habitacionOcupada)); // 👈 Corregido aquí

        // WHEN & THEN
        assertThrows(ResourceNotValidException.class, () -> {
            reservationService.createReservation(cliente.getId(), roomIds, checkIn, checkOut);
        });

        // Verificamos que NUNCA se guardó en la base de datos por seguridad
        verify(reservationRepository, never()).save(ArgumentMatchers.any(Reservation.class));
    }
}