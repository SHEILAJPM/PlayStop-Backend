package com.playstop.backend.controller;

import com.playstop.backend.dto.request.ForgotPasswordRequest;
import com.playstop.backend.dto.request.LoginRequest;
import com.playstop.backend.dto.request.RegisterRequest;
import com.playstop.backend.dto.request.ResetPasswordRequest;
import com.playstop.backend.dto.response.AuthResponse;
import com.playstop.backend.security.JwtCookieService;
import com.playstop.backend.service.AuthService;
import com.playstop.backend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
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
    private final JwtCookieService jwtCookieService;

    @PostMapping("/register/player")
    public ResponseEntity<AuthResponse> registerPlayer(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return withAuthCookie(authService.registerPlayer(request), response);
    }

    @PostMapping("/register/owner")
    public ResponseEntity<AuthResponse> registerOwner(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return withAuthCookie(authService.registerOwner(request), response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return withAuthCookie(authService.login(request), response);
    }

    // Invalida de inmediato todos los tokens ya emitidos para el usuario actual
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        authService.logout();
        jwtCookieService.clearAuthCookie(response);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody Map<String, String> body, HttpServletResponse response) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return withAuthCookie(authService.loginWithGoogle(idToken), response);
    }

    private ResponseEntity<AuthResponse> withAuthCookie(AuthResponse authResponse, HttpServletResponse response) {
        jwtCookieService.setAuthCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
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