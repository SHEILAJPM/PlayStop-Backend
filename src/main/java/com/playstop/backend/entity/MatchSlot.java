package com.playstop.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "slot_hour", nullable = false)
    private int slotHour;

    @Column(nullable = false)
    private int totalPlayers;

    @Builder.Default
    @Column(nullable = false)
    private int currentPlayers = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerPlayer;

    @Column(length = 300)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean open = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
