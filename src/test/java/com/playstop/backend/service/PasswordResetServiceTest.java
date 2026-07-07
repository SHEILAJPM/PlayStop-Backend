package com.playstop.backend.service;

import com.playstop.backend.dto.request.ForgotPasswordRequest;
import com.playstop.backend.dto.request.ResetPasswordRequest;
import com.playstop.backend.entity.PasswordResetToken;
import com.playstop.backend.entity.User;
import com.playstop.backend.exception.BusinessException;
import com.playstop.backend.repository.PasswordResetTokenRepository;
import com.playstop.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    private User owner;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(userRepository, tokenRepository, emailService, passwordEncoder);
        owner = User.builder().id(UUID.randomUUID()).name("Jugador").email("jugador@test.com").build();
    }

    @Test
    void sendResetCode_unknownEmail_doesNothingAndDoesNotThrow() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("no-existe@test.com");
        when(userRepository.findByEmailIgnoreCase("no-existe@test.com")).thenReturn(Optional.empty());

        passwordResetService.sendResetCode(request);

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void sendResetCode_knownEmail_createsTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("jugador@test.com");
        when(userRepository.findByEmailIgnoreCase("jugador@test.com")).thenReturn(Optional.of(owner));

        passwordResetService.sendResetCode(request);

        verify(tokenRepository).deleteByUser(owner);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetCode(eq("jugador@test.com"), any(), any());
    }

    @Test
    void resetPassword_codeBelongsToDifferentEmail_throwsGenericError() {
        PasswordResetToken token = PasswordResetToken.builder()
                .code("123456").user(owner).expiresAt(LocalDateTime.now().plusMinutes(10)).used(false).build();
        when(tokenRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("otro-usuario@test.com");
        request.setCode("123456");
        request.setNewPassword("nuevaClave123");

        BusinessException ex = assertThrows(BusinessException.class, () -> passwordResetService.resetPassword(request));

        assertEquals("Código inválido o ya utilizado", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_correctCodeAndEmail_updatesPassword() {
        PasswordResetToken token = PasswordResetToken.builder()
                .code("123456").user(owner).expiresAt(LocalDateTime.now().plusMinutes(10)).used(false).build();
        when(tokenRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("hashed");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("JUGADOR@test.com");
        request.setCode("123456");
        request.setNewPassword("nuevaClave123");

        passwordResetService.resetPassword(request);

        assertEquals("hashed", owner.getPassword());
        assertEquals(true, token.isUsed());
    }

    @Test
    void resetPassword_expiredCode_throws() {
        PasswordResetToken token = PasswordResetToken.builder()
                .code("123456").user(owner).expiresAt(LocalDateTime.now().minusMinutes(1)).used(false).build();
        when(tokenRepository.findByCodeAndUsedFalse("123456")).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("jugador@test.com");
        request.setCode("123456");
        request.setNewPassword("nuevaClave123");

        assertThrows(BusinessException.class, () -> passwordResetService.resetPassword(request));
    }
}
