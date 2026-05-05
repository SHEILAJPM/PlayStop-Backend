package com.playstop.backend.service;

import com.playstop.backend.entity.Reservation;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReservationRepository reservationRepository;
    private final EmailService emailService;

    // Corre cada 5 minutos
    @Scheduled(fixedDelay = 300000)
    public void sendReminders() {
        int currentHour = LocalTime.now().getHour();
        int targetHour = currentHour + 1;
        LocalDate today = LocalDate.now();

        List<Reservation> upcoming = reservationRepository
                .findByDateAndSlotHourAndStatus(today, targetHour, ReservationStatus.CONFIRMED);

        for (Reservation r : upcoming) {
            emailService.sendReservationReminder(
                r.getUser().getEmail(),
                r.getUser().getName(),
                r.getCourt().getName(),
                r.getDate().toString(),
                r.getSlotHour() + ":00 - " + (r.getSlotHour() + 1) + ":00"
            );
        }
    }
}