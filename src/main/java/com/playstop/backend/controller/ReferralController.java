package com.playstop.backend.controller;

import com.playstop.backend.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyReferralInfo() {
        return ResponseEntity.ok(referralService.getMyReferralInfo());
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> applyCode(@RequestBody Map<String, String> body) {
        referralService.applyReferralCode(body.get("code"));
        return ResponseEntity.ok(Map.of("message", "Código aplicado correctamente"));
    }
}
