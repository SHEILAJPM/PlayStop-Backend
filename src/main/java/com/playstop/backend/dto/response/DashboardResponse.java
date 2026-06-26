package com.playstop.backend.dto.response;

import java.util.List;

public record DashboardResponse(
    long reservasActivas,
    long reservasMes,
    double ingresosMes,
    double ocupacionPromedio,
    List<ReservationResponse> proximasReservas
) {}
