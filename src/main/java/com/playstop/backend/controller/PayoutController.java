package com.playstop.backend.controller;

import com.playstop.backend.dto.request.PayoutRequestDto;
import com.playstop.backend.dto.response.PayoutResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping("/api/payouts/balance")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(Map.of("availableBalance", payoutService.getAvailableBalance(owner)));
    }

    @PostMapping("/api/payouts")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<PayoutResponse> createPayoutRequest(@Valid @RequestBody PayoutRequestDto dto) {
        return ResponseEntity.ok(payoutService.createPayoutRequest(dto));
    }

    @GetMapping("/api/payouts/my")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<PayoutResponse>> getMyPayoutRequests() {
        return ResponseEntity.ok(payoutService.getMyPayoutRequests());
    }

    @GetMapping("/api/admin/payouts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PayoutResponse>> getAllPayoutRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(payoutService.getAllPayoutRequests(status));
    }

    @PatchMapping("/api/admin/payouts/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayoutResponse> markAsPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(payoutService.markAsPaid(id));
    }

    @PatchMapping("/api/admin/payouts/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayoutResponse> reject(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(payoutService.reject(id, body.getOrDefault("reason", "No especificado")));
    }
}
