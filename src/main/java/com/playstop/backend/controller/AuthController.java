package com.playstop.backend.controller;

import com.playstop.backend.dto.request.ForgotPasswordRequest;
import com.playstop.backend.dto.request.LoginRequest;
import com.playstop.backend.dto.request.RegisterRequest;
import com.playstop.backend.dto.request.ResetPasswordRequest;
import com.playstop.backend.dto.response.AuthResponse;
import com.playstop.backend.service.AuthService;
import com.playstop.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register/player")
    public ResponseEntity<AuthResponse> registerPlayer(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerPlayer(request));
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponse> registerOwner(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerOwner(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ✅ Solicitar código de recuperación
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetCode(request);
        return ResponseEntity.ok(Map.of(
            "message", "Código de verificación enviado a tu correo"
        ));
    }

    // ✅ Restablecer contraseña con el código
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
            "message", "Contraseña actualizada correctamente"
        ));
    }
}