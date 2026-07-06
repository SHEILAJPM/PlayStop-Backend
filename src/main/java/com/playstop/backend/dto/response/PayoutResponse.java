package com.playstop.backend.dto.response;

import com.playstop.backend.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayoutResponse {
    private UUID id;
    private BigDecimal amount;
    private String method;
    private String holderName;
    private String phoneNumber;
    private String bankName;
    private String accountType;
    private String accountNumber;
    private PayoutStatus status;
    private String adminNotes;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
    // Solo se llenan en las respuestas del panel de admin
    private String ownerName;
    private String ownerEmail;
}
