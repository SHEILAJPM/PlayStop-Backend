package com.playstop.backend.dto.response;

public record StatisticsResponse(
    long totalUsuarios,
    long totalCanchas,
    long totalReservas,
    long reservasHoy,
    double ingresosTotales,
    double ingresosMes
) {}
