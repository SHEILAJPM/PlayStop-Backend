package com.playstop.backend.controller;

import com.playstop.backend.entity.User;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final UserRepository userRepository;

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().build();
        if (token.equals(user.getFcmToken())) return ResponseEntity.ok().build();
        user.setFcmToken(token);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
