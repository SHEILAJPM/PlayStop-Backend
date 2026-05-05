package com.playstop.backend.controller;

import com.playstop.backend.dto.request.CourtRequest;
import com.playstop.backend.dto.response.CourtResponse;
import com.playstop.backend.dto.response.SlotResponse;
import com.playstop.backend.service.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @GetMapping
    public ResponseEntity<List<CourtResponse>> getAllCourts() {
        return ResponseEntity.ok(courtService.getAllCourts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourtResponse> getCourtById(@PathVariable UUID id) {
        return ResponseEntity.ok(courtService.getCourtById(id));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(courtService.getAvailableSlots(id, date));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.createCourt(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID id,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(courtService.updateCourt(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourt(@PathVariable UUID id) {
        courtService.deleteCourt(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<CourtResponse>> getMyCourts() {
        return ResponseEntity.ok(courtService.getMyCourts());
    }
}