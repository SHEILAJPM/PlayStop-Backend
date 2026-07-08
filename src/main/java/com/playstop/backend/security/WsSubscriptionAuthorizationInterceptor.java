package com.playstop.backend.security;

import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.ReservationRepository;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * El handshake (WsAuthHandshakeHandler) ya autentica al usuario, pero eso solo
 * prueba quién es, no a qué tiene acceso. Sin este interceptor, cualquier
 * usuario autenticado que conozca (o adivine) el UUID de una reserva o de
 * otro usuario podría suscribirse directamente a su topic y leer mensajes o
 * notificaciones ajenas, aunque ChatService sí valide el acceso al enviar.
 * Aquí se aplica la misma regla de acceso también al lado de la lectura
 * (SUBSCRIBE), y cualquier destino no reconocido se rechaza por defecto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_USER_CHANNEL = Pattern.compile("^/topic/chat/([^/]+)/user/([^/]+)$");
    private static final Pattern CHAT_TYPING = Pattern.compile("^/topic/chat/([^/]+)/typing$");
    private static final Pattern CHAT_MESSAGES = Pattern.compile("^/topic/chat/([^/]+)$");
    private static final Pattern NOTIFICATIONS = Pattern.compile("^/topic/notifications/([^/]+)$");

    private final ReservationRepository reservationRepository;

    @Override
    public Message<?> preSend(@Nonnull Message<?> message, @Nonnull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        User user = extractUser(accessor.getUser());

        if (destination == null || user == null || !isAuthorized(destination, user)) {
            log.warn("Suscripción WebSocket rechazada: destino={}, usuario={}",
                    destination, user != null ? user.getId() : "anónimo");
            throw new MessagingException("No autorizado para suscribirse a " + destination);
        }
        return message;
    }

    private boolean isAuthorized(String destination, User user) {
        Matcher m = CHAT_USER_CHANNEL.matcher(destination);
        if (m.matches()) return matchesUserId(m.group(2), user);

        m = CHAT_TYPING.matcher(destination);
        if (m.matches()) return hasAccessToReservation(m.group(1), user);

        m = CHAT_MESSAGES.matcher(destination);
        if (m.matches()) return hasAccessToReservation(m.group(1), user);

        m = NOTIFICATIONS.matcher(destination);
        if (m.matches()) return matchesUserId(m.group(1), user);

        // Destino no reconocido: fail-closed.
        return false;
    }

    private boolean hasAccessToReservation(String reservationIdRaw, User user) {
        UUID reservationId = parseUuid(reservationIdRaw);
        if (reservationId == null) return false;

        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId).orElse(null);
        if (reservation == null) return false;

        boolean isPlayer = reservation.getUser().getId().equals(user.getId());
        boolean isOwner = reservation.getCourt().getOwner().getId().equals(user.getId());
        return isPlayer || isOwner;
    }

    private boolean matchesUserId(String userIdRaw, User user) {
        UUID userId = parseUuid(userIdRaw);
        return userId != null && userId.equals(user.getId());
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private User extractUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
