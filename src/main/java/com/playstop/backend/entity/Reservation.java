package com.playstop.backend.entity;

import com.playstop.backend.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"court_id", "date", "slot_hour"}),
    indexes = {
        @Index(name = "idx_reservations_user_id", columnList = "user_id"),
        @Index(name = "idx_reservations_court_id", columnList = "court_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "slot_hour", nullable = false)
    private int slotHour;

    @Builder.Default
    @Column(name = "duration_hours", nullable = false)
    private int durationHours = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = ReservationStatus.PENDING;
    }
}