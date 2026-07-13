package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.*;
import com.example.hotel_management_system.repositories.PaymentRepository;
import com.example.hotel_management_system.repositories.ReservationRepository;
import com.example.hotel_management_system.services.payment.PaymentGateway;
import com.example.hotel_management_system.services.payment.PaymentGatewayResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Debería crear pago con tarjeta y confirmar la reserva automáticamente")
    void createPayment_WithCard_CompletesAndConfirmsReservation() {
        UUID reservationId = UUID.randomUUID();
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .user(guest)
                .totalPrice(new BigDecimal("250.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.existsByReservationId(reservationId)).thenReturn(false);
        when(paymentRepository.save(ArgumentMatchers.any(Payment.class))).thenAnswer(inv -> {
            Payment saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(paymentGateway.process(ArgumentMatchers.any(Payment.class)))
                .thenReturn(PaymentGatewayResult.builder()
                        .status(PaymentStatus.COMPLETED)
                        .message("OK")
                        .build());

        Payment result = paymentService.createPayment(reservationId, PaymentMethod.CARD, guest);

        assertThat(result.getAmount(), comparesEqualTo(new BigDecimal("250.00")));
        assertThat(result.getStatus(), is(PaymentStatus.COMPLETED));
        assertThat(result.getPaymentDate(), is(notNullValue()));
        assertThat(reservation.getStatus(), is(ReservationStatus.CONFIRMED));
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("Debería crear pago en efectivo quedando pendiente de confirmación")
    void createPayment_WithCash_StaysPending() {
        UUID reservationId = UUID.randomUUID();
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .user(guest)
                .totalPrice(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.existsByReservationId(reservationId)).thenReturn(false);
        when(paymentRepository.save(ArgumentMatchers.any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.process(ArgumentMatchers.any(Payment.class)))
                .thenReturn(PaymentGatewayResult.builder()
                        .status(PaymentStatus.PENDING)
                        .message("Pendiente recepción")
                        .build());

        Payment result = paymentService.createPayment(reservationId, PaymentMethod.CASH, guest);

        assertThat(result.getStatus(), is(PaymentStatus.PENDING));
        assertThat(reservation.getStatus(), is(ReservationStatus.PENDING));
        verify(reservationRepository, never()).save(reservation);
    }

    @Test
    @DisplayName("Debería rechazar pago duplicado para la misma reserva")
    void createPayment_ThrowsWhenPaymentAlreadyExists() {
        UUID reservationId = UUID.randomUUID();
        User guest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .user(guest)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.existsByReservationId(reservationId)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () ->
                paymentService.createPayment(reservationId, PaymentMethod.CARD, guest));
    }

    @Test
    @DisplayName("Debería denegar pago de reserva ajena a un huésped")
    void createPayment_ThrowsAccessDenied_ForOtherUsersReservation() {
        UUID reservationId = UUID.randomUUID();
        User owner = User.builder().id(UUID.randomUUID()).build();
        User otherGuest = User.builder().id(UUID.randomUUID()).role(Role.GUEST).build();
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .user(owner)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(AccessDeniedException.class, () ->
                paymentService.createPayment(reservationId, PaymentMethod.CARD, otherGuest));
    }

    @Test
    @DisplayName("Staff debería confirmar pago en efectivo pendiente")
    void updatePaymentStatus_StaffConfirmsCashPayment() {
        UUID paymentId = UUID.randomUUID();
        User receptionist = User.builder().id(UUID.randomUUID()).role(Role.RECEPTIONIST).build();
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .status(ReservationStatus.PENDING)
                .build();
        Payment payment = Payment.builder()
                .id(paymentId)
                .reservation(reservation)
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment result = paymentService.updatePaymentStatus(paymentId, PaymentStatus.COMPLETED, receptionist);

        assertThat(result.getStatus(), is(PaymentStatus.COMPLETED));
        assertThat(reservation.getStatus(), is(ReservationStatus.CONFIRMED));
    }

    @Test
    @DisplayName("Debería rechazar transición de estado inválida en pago")
    void updatePaymentStatus_ThrowsOnInvalidTransition() {
        UUID paymentId = UUID.randomUUID();
        User admin = User.builder().role(Role.ADMIN).build();
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.FAILED)
                .reservation(Reservation.builder().build())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(ResourceNotValidException.class, () ->
                paymentService.updatePaymentStatus(paymentId, PaymentStatus.REFUNDED, admin));
    }

    private static org.hamcrest.Matcher<Object> notNullValue() {
        return org.hamcrest.Matchers.notNullValue();
    }
}
