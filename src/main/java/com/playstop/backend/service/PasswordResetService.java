package com.playstop.backend.service;

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

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendResetCode(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese email"));

        // Eliminar tokens anteriores del usuario
        tokenRepository.deleteByUser(user);

        // Generar código de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(999999));

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
        PasswordResetToken token = tokenRepository.findByCodeAndUsedFalse(request.getCode())
                .orElseThrow(() -> new RuntimeException("Código inválido o ya utilizado"));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new RuntimeException("El código ha expirado, solicita uno nuevo");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Marcar token como usado
        token.setUsed(true);
        tokenRepository.save(token);
    }
}