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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("Debería crear una reserva correctamente sin marcar habitaciones como ocupadas")
    void createReservation_Success() {
        User cliente = User.builder().id(UUID.randomUUID()).name("Carlos").role(Role.GUEST).build();
        List<Long> roomIds = List.of(1L);
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(3);

        RoomType tipoSuite = RoomType.builder()
                .id(1L)
                .name("Suite")
                .pricePerNight(new BigDecimal("100.00"))
                .build();

        Room habitacion = Room.builder()
                .id(1L)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .roomType(tipoSuite)
                .build();

        when(userRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(habitacion));
        when(reservationRepository.existsOverlappingReservation(
                eq(roomIds), eq(checkIn), eq(checkOut), anyCollection(), isNull()))
                .thenReturn(false);
        when(reservationRepository.save(ArgumentMatchers.any(Reservation.class))).thenAnswer(inv -> {
            Reservation saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Reservation resultado = reservationService.createReservation(
                cliente.getId(), roomIds, checkIn, checkOut, cliente);

        assertThat(resultado.getTotalPrice(), comparesEqualTo(new BigDecimal("300.00")));
        assertThat(resultado.getStatus(), is(ReservationStatus.PENDING));
        assertThat(habitacion.getStatus(), is(RoomStatus.AVAILABLE));
        verify(roomRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Debería rechazar reserva cuando hay solapamiento de fechas")
    void createReservation_ThrowsException_WhenDatesOverlap() {
        User cliente = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        RoomType tipo = RoomType.builder().pricePerNight(new BigDecimal("80.00")).build();
        Room habitacion = Room.builder().id(2L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(tipo).build();
        List<Long> roomIds = List.of(2L);
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(2);

        when(userRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(habitacion));
        when(reservationRepository.existsOverlappingReservation(
                eq(roomIds), eq(checkIn), eq(checkOut), anyCollection(), isNull()))
                .thenReturn(true);

        assertThrows(ResourceNotValidException.class, () ->
                reservationService.createReservation(cliente.getId(), roomIds, checkIn, checkOut, cliente));

        verify(reservationRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Debería rechazar que un huésped cree reserva para otro usuario")
    void createReservation_ThrowsAccessDenied_WhenGuestCreatesForOther() {
        UUID targetId = UUID.randomUUID();
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();

        assertThrows(AccessDeniedException.class, () ->
                reservationService.createReservation(targetId, List.of(1L), LocalDate.now(),
                        LocalDate.now().plusDays(1), guest));
    }

    @Test
    @DisplayName("Debería listar solo las reservas del huésped autenticado")
    void getAllReservations_ReturnsOwnForGuest() {
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Pageable pageable = PageRequest.of(0, 10);
        Reservation reserva = Reservation.builder().id(UUID.randomUUID()).build();
        Page<Reservation> page = new PageImpl<>(List.of(reserva));

        when(reservationRepository.findByUserId(guest.getId(), pageable)).thenReturn(page);

        Page<Reservation> result = reservationService.getAllReservations(guest, pageable);

        assertThat(result.getContent(), hasSize(1));
        verify(reservationRepository, never()).findAll(ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("Debería listar todas las reservas para staff")
    void getAllReservations_ReturnsAllForStaff() {
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(List.of(new Reservation()));

        when(reservationRepository.findAll(pageable)).thenReturn(page);

        Page<Reservation> result = reservationService.getAllReservations(admin, pageable);

        assertThat(result.getContent(), hasSize(1));
    }

    @Test
    @DisplayName("Debería permitir check-in y marcar habitaciones como ocupadas")
    void updateReservationStatus_CheckIn_OccupiesRooms() {
        UUID reservationId = UUID.randomUUID();
        User receptionist = User.builder().id(UUID.randomUUID()).role(Role.RECEPTIONIST).build();
        Room room = Room.builder().id(1L).roomNumber("101").status(RoomStatus.AVAILABLE).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.CONFIRMED)
                .rooms(List.of(room))
                .user(receptionist)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.updateReservationStatus(
                reservationId, ReservationStatus.CHECKED_IN, receptionist);

        assertThat(result.getStatus(), is(ReservationStatus.CHECKED_IN));
        assertThat(room.getStatus(), is(RoomStatus.OCCUPIED));
        verify(roomRepository).saveAll(List.of(room));
    }

    @Test
    @DisplayName("Debería cancelar reserva pendiente y liberar habitaciones")
    void cancelReservation_Success() {
        UUID reservationId = UUID.randomUUID();
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Room room = Room.builder().id(1L).status(RoomStatus.AVAILABLE).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.PENDING)
                .user(guest)
                .rooms(List.of(room))
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(reservationId, guest);

        assertThat(reservation.getStatus(), is(ReservationStatus.CANCELLED));
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("Debería rechazar transición de estado inválida")
    void updateReservationStatus_ThrowsOnInvalidTransition() {
        UUID reservationId = UUID.randomUUID();
        User admin = User.builder().role(Role.ADMIN).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.PENDING)
                .rooms(List.of())
                .user(admin)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(ResourceNotValidException.class, () ->
                reservationService.updateReservationStatus(reservationId, ReservationStatus.CHECKED_OUT, admin));
    }
}
