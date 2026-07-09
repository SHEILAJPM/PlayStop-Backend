package com.playstop.backend.service;

import com.playstop.backend.dto.request.ChatMessageRequest;
import com.playstop.backend.dto.response.ChatMessageResponse;
import com.playstop.backend.dto.response.NotificationResponse;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.ReservationMessage;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.ReservationMessageRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ReservationMessageRepository messageRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final ReservationService reservationService;
    private final CourtAccessService courtAccessService;

    private static final int RATE_LIMIT = 10;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Palabras prohibidas (español, inglés y jerga peruana ampliada) ────────
    private static final List<String> BANNED_WORDS = List.of(
        // ── Español general ──
        "mierda","puta","puto","coño","joder","gilipollas","imbécil","imbecil",
        "maricón","maricon","marica","zorra","perra","cabrón","cabron","cabrona",
        "idiota","estúpido","estupido","estúpida","estupida",
        "pendejo","pendeja","culero","culera","chingada","chingado","pinche",
        "huevón","huevon","huevona","mamón","mamon","mamona","verga","culo",
        "hijo de puta","hijodeputa","hija de puta","hdp","ctm","csm","cstm",
        "conchasumadre","conchetumadre","conchatumadre","conchatumare",
        "weon","weón","weona","cagada","cagado","cagón","cagona",
        "forro","pelotudo","pelotuda","boludo","boluda","concha",
        "lacra","maldito","maldita","bastardo","bastarda","desgraciado","desgraciada",
        "puerco","puerca","cerdo","cerda","putamadre","puta madre",
        "comemierda","maldita madre","hijueputa","malparido","malparida",
        // ── Jerga peruana intensificada ──
        "cholo","chola","cholo de mierda","serrano","serranazo",
        "conchudo","conchuda","carajo","cojudo","cojuda","cojudazo","cojudísimo",
        "cojudes","recontra cojudo","recontra cojuda","recontra",
        "cabro","baboso","babosa","chuchumeco","chuchumeca",
        "sinvergüenza","sinverguenza","miserable","soplón","soplon",
        "bagre","choborra","huasca","porquería","porqueria",
        "zángano","zangano","ratero","ratera","chismoso","chismosa",
        "metiche","gorrero","gorrona","piraña","pirana","piraña de mierda",
        "paltear","palteado","palteada","bruto","bruta","tarado","tarada",
        "pichula","picha","pichulita",
        "culiao","culiado","culiada",
        "cochino","cochina","chancho","chancha","chancho culero",
        "mamahuevo","mama huevo",
        "huevadas","huevada","huevadita",
        "piruja","pirujada","pirujo",
        "cachero","cachera","cachudo","cachuda",
        "faite","faitón","choro","chorón",
        "ladrón","ladra","ladrona","ladrona de mierda",
        "sapo","sapito","buchón","buchona",
        "toco","toca","mongólico","mongólica","mongolito","mongolita",
        "subnormal","débil mental",
        "pajero","pajera","pajero culero",
        "inútil","animal","bestia","salvaje","ignorante",
        "mierdera","mierdero","mierdecita",
        "ñangara","malandra","malandrín",
        "huachafa","huachafo","huachafería",
        "ñato","ñata","cuatro ojos","gordo de mierda","flaco de mierda",
        "loco de mierda","psicópata","esquizofrénico",
        "cara de culo","saco largo","vago de mierda","vagabundo","vagabunda",
        "mendigo","mendiga","mendicante",
        "mentiroso","mentirosa","hipócrita","traicionero","traicionera",
        "vendido","vendida","resentido","resentida","amargado","amargada",
        "feo de mierda","fea de mierda","engendro","feto",
        "delincuente","delincuenta","criminal",
        "putear","puteado","puteada",
        "chanta","chantaje",
        "tonto","tonta","tontazo","tontaza",
        // ── Inglés ──
        "fuck","shit","bitch","asshole","bastard","cunt","dick","cock",
        "pussy","nigger","faggot","whore","slut","retard","motherfucker",
        "fucker","damn","crap","bullshit","jackass","douchebag","moron",
        "idiot","stupid","loser","jerk","twat","wanker","prick","arse",
        "son of a bitch","son of bitch","wtf","stfu","gtfo"
    );

    private static final Pattern BANNED_PATTERN;
    static {
        StringBuilder sb = new StringBuilder("(?i)(?<![\\p{L}\\p{N}])(");
        for (int i = 0; i < BANNED_WORDS.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(Pattern.quote(BANNED_WORDS.get(i)));
        }
        sb.append(")");
        BANNED_PATTERN = Pattern.compile(sb.toString());
    }

    // ── Operaciones públicas ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID reservationId, User requester) {
        Reservation reservation = getReservationAndCheckAccess(reservationId, requester);
        return messageRepository
            .findByReservationIdOrderBySentAtAsc(reservation.getId())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID reservationId, ChatMessageRequest req, User sender) {
        Reservation reservation = getReservationAndCheckAccess(reservationId, sender);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No se pueden enviar mensajes en una reserva cancelada");
        }

        // ── Verificar ban permanente ──
        if (sender.isChatPermanentlyBanned()) {
            broadcastSystemMessage(reservationId,
                "⛔ Tu cuenta está bloqueada permanentemente del chat por incumplir las normas.",
                sender.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta bloqueada permanentemente del chat");
        }

        // ── Verificar suspensión activa ──
        if (sender.getChatSuspendedUntil() != null
                && LocalDateTime.now().isBefore(sender.getChatSuspendedUntil())) {
            String hasta = sender.getChatSuspendedUntil().format(DATE_FMT);
            broadcastSystemMessage(reservationId,
                "🚫 Tu cuenta está suspendida hasta el " + hasta + ". No puedes enviar mensajes.",
                sender.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta suspendida hasta " + hasta);
        }

        // ── Rate limiting ──
        long recent = messageRepository.countByReservationIdAndSenderIdAndSentAtAfter(
            reservationId, sender.getId(), LocalDateTime.now().minusMinutes(1)
        );
        if (recent >= RATE_LIMIT) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Estás enviando mensajes muy rápido. Espera un momento.");
        }

        String raw     = req.getContent().trim();
        boolean blocked = containsBannedWords(raw);
        String content  = blocked ? censorText(raw) : raw;

        String role = sender.getId().equals(reservation.getUser().getId()) ? "PLAYER" : "OWNER";

        ReservationMessage msg = ReservationMessage.builder()
            .reservationId(reservationId)
            .senderId(sender.getId())
            .senderName(sender.getName())
            .senderRole(role)
            .content(content)
            .blocked(blocked)
            .build();

        ChatMessageResponse saved = toResponse(messageRepository.save(msg));

        // ── Aplicar moderación si hubo palabras prohibidas ──
        if (blocked) {
            String action = applyModeration(sender, countBannedWords(raw), reservationId);
            saved = ChatMessageResponse.builder()
                .id(saved.getId())
                .reservationId(saved.getReservationId())
                .senderId(saved.getSenderId())
                .senderName(saved.getSenderName())
                .senderRole(saved.getSenderRole())
                .content(saved.getContent())
                .blocked(saved.isBlocked())
                .sentAt(saved.getSentAt())
                .moderationAction(action)
                .suspendedUntil(sender.getChatSuspendedUntil())
                .build();
        }

        // ── Notificar al otro participante ──
        User recipient = role.equals("PLAYER")
            ? reservation.getCourt().getOwner()
            : reservation.getUser();

        String courtName = reservation.getCourt().getName();
        String preview   = content.length() > 60 ? content.substring(0, 60) + "…" : content;

        NotificationResponse notification = NotificationResponse.builder()
            .type("CHAT_MESSAGE")
            .title("Nuevo mensaje de " + sender.getName())
            .preview(preview)
            .courtName(courtName)
            .reservationId(reservationId)
            .senderName(sender.getName())
            .senderRole(role)
            .build();

        messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), notification);

        sendChatEmail(recipient.getEmail(), recipient.getName(), sender.getName(), courtName, preview);

        return saved;
    }

    // ── Moderación interna ────────────────────────────────────────────────────

    /**
     * Aplica la escalada de moderación según el historial del sender.
     * Devuelve el código de acción realizada, o null si aún no se alcanza umbral.
     */
    private String applyModeration(User sender, int newBadWords, UUID reservationId) {
        int total = sender.getChatViolationCount() + newBadWords;

        // Paso 0 → advertencia (≥5 palabras acumuladas)
        if (!sender.isChatWarningIssued()) {
            if (total >= 5) {
                sender.setChatWarningIssued(true);
                sender.setChatViolationCount(0);
                userRepository.save(sender);
                broadcastSystemMessage(reservationId,
                    "⚠️ ADVERTENCIA: " + sender.getName() +
                    " ha usado lenguaje inapropiado. " +
                    "Si continúa, su cuenta será suspendida.", null);
                return "WARNING";
            }
            sender.setChatViolationCount(total);
            userRepository.save(sender);
            return null;
        }

        // Paso 1 → suspensión 1 día (≥10 palabras después de advertencia)
        if (sender.getChatSuspensionCount() == 0) {
            if (total >= 10) {
                LocalDateTime until = LocalDateTime.now().plusDays(1);
                sender.setChatSuspendedUntil(until);
                sender.setChatSuspensionCount(1);
                sender.setChatViolationCount(0);
                userRepository.save(sender);
                broadcastSystemMessage(reservationId,
                    "🚫 " + sender.getName() +
                    " ha sido suspendido por 1 día (hasta el " + until.format(DATE_FMT) +
                    ") por uso reiterado de lenguaje inapropiado.", null);
                return "SUSPENDED_1D";
            }
            sender.setChatViolationCount(total);
            userRepository.save(sender);
            return null;
        }

        // Paso 2 → suspensión 5 días (≥10 palabras tras cumplir suspensión de 1 día)
        if (sender.getChatSuspensionCount() == 1) {
            if (total >= 10) {
                LocalDateTime until = LocalDateTime.now().plusDays(5);
                sender.setChatSuspendedUntil(until);
                sender.setChatSuspensionCount(2);
                sender.setChatViolationCount(0);
                userRepository.save(sender);
                broadcastSystemMessage(reservationId,
                    "🚫 " + sender.getName() +
                    " ha sido suspendido por 5 días (hasta el " + until.format(DATE_FMT) +
                    ") por incumplimiento reiterado de las normas.", null);
                return "SUSPENDED_5D";
            }
            sender.setChatViolationCount(total);
            userRepository.save(sender);
            return null;
        }

        // Paso 3 → bloqueo permanente (cualquier palabra tras cumplir suspensión de 5 días)
        if (sender.getChatSuspensionCount() >= 2) {
            sender.setChatPermanentlyBanned(true);
            sender.setChatViolationCount(0);
            userRepository.save(sender);
            broadcastSystemMessage(reservationId,
                "⛔ La cuenta de " + sender.getName() +
                " ha sido bloqueada permanentemente del chat por incumplimiento grave de las normas.", null);
            // Cancelar reservas del día actual del usuario baneado
            reservationService.cancelTodayReservationsForUser(sender.getId());
            return "BANNED";
        }

        return null;
    }

    /**
     * Envía un mensaje de sistema al topic del chat.
     * Si senderId != null, también lo envía al canal privado del usuario.
     */
    private void broadcastSystemMessage(UUID reservationId, String text, UUID senderId) {
        ChatMessageResponse sys = ChatMessageResponse.builder()
            .senderRole("SYSTEM")
            .senderName("Sistema")
            .content(text)
            .sentAt(LocalDateTime.now())
            .build();

        if (senderId != null) {
            // Solo al sender (canal privado por reserva+usuario)
            messagingTemplate.convertAndSend(
                "/topic/chat/" + reservationId + "/user/" + senderId, sys);
        } else {
            // A todos los participantes
            messagingTemplate.convertAndSend("/topic/chat/" + reservationId, sys);
        }
    }

    @Async
    protected void sendChatEmail(String toEmail, String toName,
                                  String senderName, String courtName, String preview) {
        try {
            emailService.sendChatNotificationEmail(toEmail, toName, senderName, courtName, preview);
        } catch (Exception e) {
            log.warn("No se pudo enviar email de notificación de chat: {}", e.getMessage());
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Reservation getReservationAndCheckAccess(UUID reservationId, User user) {
        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        boolean isPlayer = reservation.getUser().getId().equals(user.getId());
        boolean isOwner  = courtAccessService.canManageCourt(user, reservation.getCourt());

        if (!isPlayer && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes acceso al chat de esta reserva");
        }
        return reservation;
    }

    private boolean containsBannedWords(String text) {
        return BANNED_PATTERN.matcher(text).find();
    }

    private int countBannedWords(String text) {
        Matcher m = BANNED_PATTERN.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private String censorText(String text) {
        return BANNED_PATTERN.matcher(text).replaceAll(m -> "*".repeat(m.group().length()));
    }

    private ChatMessageResponse toResponse(ReservationMessage msg) {
        return ChatMessageResponse.builder()
            .id(msg.getId())
            .reservationId(msg.getReservationId())
            .senderId(msg.getSenderId())
            .senderName(msg.getSenderName())
            .senderRole(msg.getSenderRole())
            .content(msg.getContent())
            .blocked(msg.isBlocked())
            .sentAt(msg.getSentAt())
            .build();
    }
}
