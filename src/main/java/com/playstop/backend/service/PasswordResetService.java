package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.request.ForgotPasswordRequest;
import com.playstop.backend.dto.request.ResetPasswordRequest;
import com.playstop.backend.entity.PasswordResetToken;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.PasswordResetTokenRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    // El RateLimitingFilter ya limita intentos por IP, pero un atacante que
    // reparta las peticiones entre varias IPs podría igual agotar el espacio
    // de 1 millón de códigos de 6 dígitos dentro de la ventana de 15 minutos.
    // Este contador, por email objetivo (no por IP), cierra ese hueco: tras
    // demasiados códigos incorrectos seguidos, hay que pedir uno nuevo.
    private static final int MAX_FAILED_ATTEMPTS = 8;
    private final ConcurrentHashMap<String, AtomicInteger> failedAttemptsByEmail = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendResetCode(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        failedAttemptsByEmail.remove(normalizedEmail);

        // No revela si el email existe o no: si no hay cuenta, simplemente no
        // se genera código ni se envía correo, pero el endpoint responde igual.
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim()).orElse(null);
        if (user == null) return;

        // Eliminar tokens anteriores del usuario
        tokenRepository.deleteByUser(user);

        // Generar código de 6 dígitos
        String code = String.format("%06d", RANDOM.nextInt(999999));

        PasswordResetToken token = PasswordResetToken.builder()
                .code(code)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        tokenRepository.save(token);

        // Enviar email con el código
        emailService.sendPasswordResetCode(user.getEmail(), user.getName(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        AtomicInteger attempts = failedAttemptsByEmail.computeIfAbsent(normalizedEmail, e -> new AtomicInteger(0));
        if (attempts.get() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos con este correo. Solicita un nuevo código.");
        }

        PasswordResetToken token = tokenRepository.findByCodeAndUsedFalse(request.getCode())
                .orElseGet(() -> {
                    attempts.incrementAndGet();
                    throw new BusinessException("Código inválido o ya utilizado");
                });

        if (!token.getUser().getEmail().equalsIgnoreCase(request.getEmail().trim())) {
            attempts.incrementAndGet();
            // Mismo mensaje genérico que "código inválido" — no revela que el
            // código existe pero pertenece a otro usuario.
            throw new BusinessException("Código inválido o ya utilizado");
        }

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            attempts.incrementAndGet();
            throw new BusinessException("El código ha expirado, solicita uno nuevo");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Invalida cualquier token emitido antes del reseteo (ej. uno robado)
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Marcar token como usado
        token.setUsed(true);
        tokenRepository.save(token);
        failedAttemptsByEmail.remove(normalizedEmail);
    }
}