package com.playstop.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_messages", indexes = {
    @Index(name = "idx_msg_reservation", columnList = "reservation_id"),
    @Index(name = "idx_msg_sent_at",     columnList = "sent_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    /** PLAYER | OWNER */
    @Column(name = "sender_role", nullable = false, length = 10)
    private String senderRole;

    /** Contenido visible (puede estar censurado con ***) */
    @Column(nullable = false, length = 500)
    private String content;

    /** true si el mensaje fue bloqueado por el filtro */
    @Column(nullable = false)
    private boolean blocked;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}
