package com.playstop.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private String type;          // "CHAT_MESSAGE"
    private String title;         // "Nuevo mensaje de Juan"
    private String preview;       // primeros 60 chars del mensaje
    private String courtName;
    private UUID reservationId;
    private String senderName;
    private String senderRole;    // PLAYER | OWNER
}
