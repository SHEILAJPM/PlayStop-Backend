package com.playstop.backend.entity;

import com.playstop.backend.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payout_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // "YAPE_PLIN" o "BANK"
    @Column(nullable = false, length = 20)
    private String method;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "account_number")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    public void prePersist() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) this.status = PayoutStatus.PENDING;
    }
}
