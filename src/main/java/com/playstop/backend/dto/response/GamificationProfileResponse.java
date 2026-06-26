package com.playstop.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record GamificationProfileResponse(
    int totalPoints,
    int level,
    String levelName,
    int pointsToNextLevel,
    List<AchievementResponse> achievements
) {
    public record AchievementResponse(
        String id,
        String name,
        String description,
        String icon,
        int pointsReward,
        boolean unlocked,
        LocalDateTime unlockedAt
    ) {}
}
