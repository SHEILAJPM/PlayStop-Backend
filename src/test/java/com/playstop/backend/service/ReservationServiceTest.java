package com.playstop.backend.service;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cubre solo los métodos de ReservationService invocados desde el webhook de
 * Stripe (PaymentService) — la ruta donde una reserva pasa de PENDING a
 * CONFIRMED/CANCELLED según el resultado del pago.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private CourtRepository courtRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private QrService qrService;
    @Mock
    private WhatsAppService whatsAppService;
    @Mock
    private GamificationService gamificationService;

    private ReservationService reservationService;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository, courtRepository, userRepository, emailService, qrService, whatsAppService);
        ReflectionTestUtils.setField(reservationService, "gamificationService", gamificationService);

        User player = User.builder().id(UUID.randomUUID()).name("Jugador").email("jugador@test.com").phone("999999999").build();
        User owner = User.builder().id(UUID.randomUUID()).name("Dueño").email("owner@test.com").build();
        Court court = Court.builder().id(UUID.randomUUID()).name("Cancha 1").owner(owner).build();
        reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .user(player)
                .court(court)
                .date(java.time.LocalDate.now().plusDays(1))
                .slotHour(10)
                .durationHours(1)
                .totalAmount(BigDecimal.valueOf(50))
                .status(ReservationStatus.PENDING)
                .build();
    }

    @Test
    void confirmReservationPayment_pending_confirmsAndTriggersNotifications() throws Exception {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(qrService.generateQr(any())).thenReturn(new byte[]{1, 2, 3});

        reservationService.confirmReservationPayment(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(gamificationService).onReservationCreated(reservation.getUser());
        verify(emailService).sendReservationConfirmationWithQr(
                eq("jugador@test.com"), any(), any(), any(), any(), any(), any());
        verify(emailService).sendNewReservationNotificationToOwner(
                eq("owner@test.com"), any(), any(), any(), any(), any(), any(), any(), anyDouble());
    }

    @Test
    void confirmReservationPayment_notPending_isNoOp() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        reservationService.confirmReservationPayment(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(gamificationService, emailService, qrService, whatsAppService);
    }

    @Test
    void cancelExpiredReservation_pending_cancelsReservation() {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.cancelExpiredReservation(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void cancelExpiredReservation_alreadyConfirmed_isNoOp() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        reservationService.cancelExpiredReservation(reservation.getId());

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(reservationRepository, never()).save(any());
    }
}
