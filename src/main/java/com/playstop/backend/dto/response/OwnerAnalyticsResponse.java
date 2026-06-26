package com.playstop.backend.dto.response;

import java.util.List;

public record OwnerAnalyticsResponse(
    List<DailyRevenue>    ingresosPorDia,
    List<HourlyCount>     horasPico,
    List<CourtStats>      estadisticasPorCancha,
    double                ingresosTotales,
    long                  reservasTotales,
    long                  reservasMes
) {
    public record DailyRevenue(String date, double amount) {}
    public record HourlyCount(int hour, long count) {}
    public record CourtStats(String courtName, long reservas, double ingresos) {}
}
