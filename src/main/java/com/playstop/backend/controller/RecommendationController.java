package com.playstop.backend.controller;

import com.playstop.backend.entity.Court;
import com.playstop.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRecommendations() {
        List<Court> courts = recommendationService.getRecommendations();
        List<Map<String, Object>> result = courts.stream().map(c -> Map.<String, Object>of(
            "id",           c.getId(),
            "name",         c.getName(),
            "sportType",    c.getSportType(),
            "pricePerHour", c.getPricePerHour(),
            "address",      c.getAddress() != null ? c.getAddress() : "",
            "city",         c.getCity() != null ? c.getCity() : "",
            "imageUrl",     c.getImageUrl() != null ? c.getImageUrl() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
