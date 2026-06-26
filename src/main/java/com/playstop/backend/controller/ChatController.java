package com.playstop.backend.controller;

import com.playstop.backend.dto.request.ChatMessageRequest;
import com.playstop.backend.dto.response.ChatMessageResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /** REST — historial de mensajes de una reserva */
    @GetMapping("/api/chat/{reservationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }
        return ResponseEntity.ok(chatService.getMessages(reservationId, currentUser));
    }

    /** WebSocket STOMP — indicador "escribiendo..."
     *  Destino: /app/chat/{reservationId}/typing
     *  Broadcast: /topic/chat/{reservationId}/typing
     */
    @MessageMapping("/chat/{reservationId}/typing")
    public void typingIndicator(
            @DestinationVariable String reservationId,
            Principal principal) {

        if (principal == null) return;

        User sender = (User) ((org.springframework.security.authentication
            .UsernamePasswordAuthenticationToken) principal).getPrincipal();

        Map<String, String> payload = Map.of(
            "senderId",   sender.getId().toString(),
            "senderName", sender.getName()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + reservationId + "/typing", payload);
    }

    /** WebSocket STOMP — enviar mensaje
     *  Destino: /app/chat/{reservationId}/send
     *  Broadcast: /topic/chat/{reservationId}
     *
     *  Si el usuario está suspendido/baneado, el servicio lanza ResponseStatusException
     *  y el canal privado /topic/chat/{id}/user/{userId} recibe la notificación.
     */
    @MessageMapping("/chat/{reservationId}/send")
    public void sendMessage(
            @DestinationVariable String reservationId,
            @Payload @Valid ChatMessageRequest request,
            Principal principal) {

        if (principal == null) return;

        User sender = (User) ((org.springframework.security.authentication
            .UsernamePasswordAuthenticationToken) principal).getPrincipal();

        try {
            ChatMessageResponse response = chatService.sendMessage(
                UUID.fromString(reservationId), request, sender
            );
            messagingTemplate.convertAndSend("/topic/chat/" + reservationId, response);
        } catch (ResponseStatusException e) {
            // Mensaje bloqueado por moderación; el servicio ya envió la notificación privada
            log.debug("Mensaje bloqueado por moderación para sender={}: {}", sender.getId(), e.getReason());
        }
    }

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(ChatController.class);
}
