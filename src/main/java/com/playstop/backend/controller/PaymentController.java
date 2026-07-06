package com.playstop.backend.controller;

import com.playstop.backend.entity.User;
import com.playstop.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout/{reservationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @PathVariable UUID reservationId,
            @AuthenticationPrincipal User currentUser) {
        String url = paymentService.createCheckoutSession(reservationId, currentUser.getId());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
