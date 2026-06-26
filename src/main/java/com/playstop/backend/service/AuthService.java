package com.playstop.backend.service;

import com.playstop.backend.dto.request.LoginRequest;
import com.playstop.backend.dto.request.RegisterRequest;
import com.playstop.backend.dto.response.AuthResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.UserRepository;
import com.playstop.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse registerPlayer(RegisterRequest request) {
        return register(request, Role.USER);
    }

    public AuthResponse registerOwner(RegisterRequest request) {
        return register(request, Role.OWNER);
    }

    private AuthResponse register(RegisterRequest request, Role role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .build();

        userRepository.save(user);

        // ✅ Enviar email de bienvenida
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .bannedFromReservations(!user.isEnabled() || user.isChatPermanentlyBanned())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .bannedFromReservations(!user.isEnabled() || user.isChatPermanentlyBanned())
                .build();
    }

    @SuppressWarnings("unchecked")
    public AuthResponse loginWithGoogle(String idToken) {
        // Verificar el ID token contra el endpoint público de Google
        RestTemplate rest = new RestTemplate();
        Map<String, String> info;
        try {
            info = rest.getForObject(
                "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Token de Google inválido");
        }

        if (info == null || info.get("email") == null) {
            throw new RuntimeException("No se pudo obtener el email de Google");
        }

        String email    = info.get("email");
        String name     = info.get("name") != null ? info.get("name") : email;
        String googleId = info.get("sub");
        String picture  = info.get("picture");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .role(Role.USER)
                .googleId(googleId)
                .profileImageUrl(picture)
                .build();
            userRepository.save(newUser);
            emailService.sendWelcomeEmail(email, name);
            return newUser;
        });

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}