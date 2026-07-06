package com.playstop.backend.controller;

import com.playstop.backend.dto.response.SubscriptionResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<SubscriptionResponse> getMySubscription(@AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(owner));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @AuthenticationPrincipal User owner,
            @RequestBody Map<String, String> body) {
        String url = subscriptionService.createCheckoutSession(owner, body.get("plan"));
        return ResponseEntity.ok(Map.of("url", url));
    }
}
