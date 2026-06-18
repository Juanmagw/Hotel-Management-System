package com.example.hotel_management_system.services;

import com.example.hotel_management_system.exceptions.ResourceAlreadyExistsException;
import com.example.hotel_management_system.exceptions.ResourceNotFoundException;
import com.example.hotel_management_system.exceptions.ResourceNotValidException;
import com.example.hotel_management_system.model.entities.*;
import com.example.hotel_management_system.repositories.PaymentRepository;
import com.example.hotel_management_system.repositories.ReservationRepository;
import com.example.hotel_management_system.services.payment.PaymentGateway;
import com.example.hotel_management_system.services.payment.PaymentGatewayResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, Set.of(PaymentStatus.COMPLETED, PaymentStatus.FAILED),
            PaymentStatus.COMPLETED, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.REFUNDED, Set.of()
    );

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGateway paymentGateway;

    public Payment getPaymentById(UUID id, User requester) {
        Payment payment = getPaymentOrThrow(id);
        assertCanViewPayment(requester, payment);
        return payment;
    }

    public Payment getPaymentByReservationId(UUID reservationId, User requester) {
        Reservation reservation = getReservationOrThrow(reservationId);
        assertCanViewReservation(requester, reservation);

        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró pago para la reserva con ID: " + reservationId));
    }

    @Transactional
    public Payment createPayment(UUID reservationId, PaymentMethod paymentMethod, User requester) {
        Reservation reservation = getReservationOrThrow(reservationId);
        assertCanPay(requester, reservation);

        if (paymentRepository.existsByReservationId(reservationId)) {
            throw new ResourceAlreadyExistsException("La reserva ya tiene un pago registrado.");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResourceNotValidException(
                    "Solo se puede pagar una reserva en estado PENDING o CONFIRMED.");
        }

        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(reservation.getTotalPrice())
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        PaymentGatewayResult result = paymentGateway.process(payment);
        applyGatewayResult(payment, reservation, result);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment updatePaymentStatus(UUID id, PaymentStatus newStatus, User requester) {
        if (!isStaff(requester)) {
            throw new AccessDeniedException("Solo el personal del hotel puede actualizar el estado del pago");
        }

        Payment payment = getPaymentOrThrow(id);
        validateStatusTransition(payment.getStatus(), newStatus);

        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
            confirmReservationIfPending(payment.getReservation());
        }

        return paymentRepository.save(payment);
    }

    private Payment getPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
    }

    private Reservation getReservationOrThrow(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con ID: " + id));
    }

    private void applyGatewayResult(Payment payment, Reservation reservation, PaymentGatewayResult result) {
        payment.setStatus(result.status());
        if (result.status() == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
            confirmReservationIfPending(reservation);
        }
    }

    private void confirmReservationIfPending(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
        }
    }

    private void validateStatusTransition(PaymentStatus current, PaymentStatus next) {
        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new ResourceNotValidException(
                    "No se puede cambiar el estado del pago de " + current + " a " + next);
        }
    }

    private boolean isStaff(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.RECEPTIONIST;
    }

    private void assertCanViewPayment(User requester, Payment payment) {
        assertCanViewReservation(requester, payment.getReservation());
    }

    private void assertCanViewReservation(User requester, Reservation reservation) {
        if (isStaff(requester)) {
            return;
        }
        if (!requester.getId().equals(reservation.getUser().getId())) {
            throw new AccessDeniedException("No tienes permiso para consultar este pago");
        }
    }

    private void assertCanPay(User requester, Reservation reservation) {
        if (isStaff(requester)) {
            return;
        }
        if (!requester.getId().equals(reservation.getUser().getId())) {
            throw new AccessDeniedException("No puedes pagar la reserva de otro usuario");
        }
    }
}
