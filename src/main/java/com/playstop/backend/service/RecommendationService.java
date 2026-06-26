package com.playstop.backend.service;

import com.playstop.backend.entity.Court;
import com.playstop.backend.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CourtRepository courtRepository;

    public List<Court> getRecommendations() {
        return courtRepository.findByActiveTrue();
    }
}
