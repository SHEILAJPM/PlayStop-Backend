package com.playstop.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatchSlotResponse(
    UUID id,
    UUID courtId,
    String courtName,
    String courtImageUrl,
    String courtDistrict,
    String sportType,
    String organizerName,
    String organizerAvatar,
    LocalDate date,
    int slotHour,
    int totalPlayers,
    int currentPlayers,
    int spotsLeft,
    BigDecimal pricePerPlayer,
    String description,
    boolean open,
    LocalDateTime createdAt
) {}
