package com.playstop.backend.controller;

import com.playstop.backend.dto.request.MatchSlotRequest;
import com.playstop.backend.dto.response.MatchSlotResponse;
import com.playstop.backend.service.MatchSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class MatchSlotController {

    private final MatchSlotService matchSlotService;

    @GetMapping
    public ResponseEntity<List<MatchSlotResponse>> getOpenMatches() {
        return ResponseEntity.ok(matchSlotService.getOpenMatches());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MatchSlotResponse> createMatch(@Valid @RequestBody MatchSlotRequest request) {
        return ResponseEntity.ok(matchSlotService.createMatch(request));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MatchSlotResponse> joinMatch(@PathVariable UUID id) {
        return ResponseEntity.ok(matchSlotService.joinMatch(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> cancelMatch(@PathVariable UUID id) {
        matchSlotService.cancelMatch(id);
        return ResponseEntity.noContent().build();
    }
}
