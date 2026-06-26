package com.playstop.backend.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MatchSlotRequest(
    @NotNull UUID courtId,
    @NotNull LocalDate date,
    @Min(6) @Max(23) int slotHour,
    @Min(2) @Max(22) int totalPlayers,
    @NotNull @DecimalMin("1.00") BigDecimal pricePerPlayer,
    @Size(max = 300) String description
) {}
