package com.playstop.backend.service;

import com.playstop.backend.dto.response.OwnerAnalyticsResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public OwnerAnalyticsResponse getOwnerAnalytics() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        LocalDate since30 = LocalDate.now().minusDays(29);
        LocalDate since1m = LocalDate.now().withDayOfMonth(1);

        List<OwnerAnalyticsResponse.DailyRevenue> ingresosPorDia =
            reservationRepository.findDailyRevenueByOwner(owner, since30)
                .stream()
                .map(row -> new OwnerAnalyticsResponse.DailyRevenue(
                    row[0].toString(),
                    ((BigDecimal) row[1]).doubleValue()
                ))
                .toList();

        List<OwnerAnalyticsResponse.HourlyCount> horasPico =
            reservationRepository.findPeakHoursByOwner(owner)
                .stream()
                .map(row -> new OwnerAnalyticsResponse.HourlyCount(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).longValue()
                ))
                .toList();

        List<OwnerAnalyticsResponse.CourtStats> estadisticasPorCancha =
            reservationRepository.findCourtStatsByOwner(owner)
                .stream()
                .map(row -> new OwnerAnalyticsResponse.CourtStats(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((BigDecimal) row[2]).doubleValue()
                ))
                .toList();

        Object[] totals = reservationRepository.findTotalStatsByOwner(owner);
        long totalReservas = ((Number) totals[0]).longValue();
        double ingresosTotales = ((BigDecimal) totals[1]).doubleValue();

        long reservasMes = reservationRepository.countReservationsByOwnerSince(owner, since1m);

        return new OwnerAnalyticsResponse(
            ingresosPorDia,
            horasPico,
            estadisticasPorCancha,
            ingresosTotales,
            totalReservas,
            reservasMes
        );
    }
}
