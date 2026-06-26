package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID reservationId;
    private UUID senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private boolean blocked;
    private LocalDateTime sentAt;

    /** Acción de moderación aplicada en este mensaje (puede ser null) */
    private String moderationAction; // WARNING | SUSPENDED_1D | SUSPENDED_5D | BANNED

    /** Fecha hasta cuando dura la suspensión (solo cuando moderationAction es SUSPENDED_*) */
    private LocalDateTime suspendedUntil;
}
