package com.playstop.backend.service;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private WhatsAppService whatsAppService;

    private ReminderScheduler reminderScheduler;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reminderScheduler = new ReminderScheduler(reservationRepository, emailService, whatsAppService);

        User player = User.builder().id(UUID.randomUUID()).name("Jugador").email("jugador@test.com").phone("999999999").build();
        Court court = Court.builder().id(UUID.randomUUID()).name("Cancha 1").build();
        reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .user(player)
                .court(court)
                .date(LocalDate.now())
                .slotHour(15)
                .durationHours(1)
                .totalAmount(BigDecimal.valueOf(50))
                .status(ReservationStatus.CONFIRMED)
                .reminderSent(false)
                .build();
    }

    @Test
    void sendReminders_noUpcomingReservations_doesNothing() {
        when(reservationRepository.findByDateAndSlotHourAndStatusAndReminderSentFalse(any(), anyInt(), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of());

        reminderScheduler.sendReminders();

        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(emailService, whatsAppService);
    }

    @Test
    void sendReminders_upcomingReservation_marksSentAndNotifiesByEmailAndWhatsApp() {
        when(reservationRepository.findByDateAndSlotHourAndStatusAndReminderSentFalse(any(), anyInt(), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of(reservation));

        reminderScheduler.sendReminders();

        assertTrue(reservation.isReminderSent());
        verify(reservationRepository).save(reservation);
        verify(emailService).sendReservationReminder(
                eq("jugador@test.com"), eq("Jugador"), eq("Cancha 1"), any(), any());
        verify(whatsAppService).sendReservationReminder(
                eq("999999999"), eq("Jugador"), eq("Cancha 1"), any());
    }

    @Test
    void sendReminders_marksReminderSentBeforeNotifying() {
        // Evita reenvios si el scheduler se solapa o corre en varias
        // instancias: el flag debe quedar guardado antes de intentar avisar.
        when(reservationRepository.findByDateAndSlotHourAndStatusAndReminderSentFalse(any(), anyInt(), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of(reservation));

        reminderScheduler.sendReminders();

        InOrder inOrder = inOrder(reservationRepository, emailService, whatsAppService);
        inOrder.verify(reservationRepository).save(reservation);
        inOrder.verify(emailService).sendReservationReminder(any(), any(), any(), any(), any());
        inOrder.verify(whatsAppService).sendReservationReminder(any(), any(), any(), any());
    }
}
