package com.playstop.backend.controller;

import com.playstop.backend.dto.request.ChangePasswordRequest;
import com.playstop.backend.dto.request.UpdateUserRequest;
import com.playstop.backend.dto.response.UserProfileResponse;
import com.playstop.backend.dto.response.UserSearchResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.UserRepository;
import com.playstop.backend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FriendService friendService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe() {
        User user = getCurrentUser();
        return ResponseEntity.ok(toProfile(user));
    }

    @GetMapping("/search")
    public ResponseEntity<UserSearchResponse> searchByEmail(@RequestParam String email) {
        return friendService.searchByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        User user = getCurrentUser();
        user.setName(request.nombre());
        if (request.telefono() != null) user.setPhone(request.telefono());
        userRepository.save(user);
        return ResponseEntity.ok(toProfile(user));
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<UserProfileResponse> updateAvatar(@RequestBody Map<String, String> body) {
        String url = body.get("profileImageUrl");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        User user = getCurrentUser();
        user.setProfileImageUrl(url);
        userRepository.save(user);
        return ResponseEntity.ok(toProfile(user));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        if (!request.nuevaContrasena().equals(request.confirmarContrasena())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Las contraseñas no coinciden"));
        }
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.contrasenaActual(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "La contraseña actual es incorrecta"));
        }
        user.setPassword(passwordEncoder.encode(request.nuevaContrasena()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
