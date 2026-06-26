package com.playstop.backend.controller;

import com.playstop.backend.dto.response.OwnerAnalyticsResponse;
import com.playstop.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<OwnerAnalyticsResponse> getOwnerAnalytics() {
        return ResponseEntity.ok(analyticsService.getOwnerAnalytics());
    }
}
