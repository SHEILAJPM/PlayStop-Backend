package com.playstop.backend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ReservationRequest {

    @NotNull(message = "La cancha es obligatoria")
    private UUID courtId;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha no puede ser en el pasado")
    private LocalDate date;

    @Min(value = 6, message = "El slot mínimo es 6 (6AM)")
    @Max(value = 23, message = "El slot máximo es 23 (11PM)")
    private int slotHour;
}