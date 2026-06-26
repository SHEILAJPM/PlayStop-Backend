package com.playstop.backend.controller;

import com.playstop.backend.dto.response.GamificationProfileResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.UserRepository;
import com.playstop.backend.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<GamificationProfileResponse> getMyProfile() {
        User user = getCurrentUser();
        return ResponseEntity.ok(gamificationService.getProfile(user));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}
