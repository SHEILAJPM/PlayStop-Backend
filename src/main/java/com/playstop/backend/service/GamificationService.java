package com.playstop.backend.service;

import com.playstop.backend.dto.response.GamificationProfileResponse;
import com.playstop.backend.dto.response.GamificationProfileResponse.AchievementResponse;
import com.playstop.backend.entity.Achievement;
import com.playstop.backend.entity.User;
import com.playstop.backend.entity.UserAchievement;
import com.playstop.backend.entity.UserPoints;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.AchievementRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.ReviewRepository;
import com.playstop.backend.repository.UserAchievementRepository;
import com.playstop.backend.repository.UserPointsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GamificationService {

    private static final int POINTS_PER_ATTENDED = 5;

    private static final int[] LEVEL_THRESHOLDS = {0, 100, 250, 500, 1000, 2000};
    private static final String[] LEVEL_NAMES = {"Principiante", "Amateur", "Intermedio", "Avanzado", "Experto", "Leyenda"};

    private static final List<Achievement> SEED_ACHIEVEMENTS = List.of(
        Achievement.builder().id("PRIMERA_RESERVA").name("Primera Reserva")
            .description("Realizaste tu primera reserva").icon("🎯").pointsReward(10).build(),
        Achievement.builder().id("PARTIDOS_10").name("Jugador Activo")
            .description("10 partidos completados").icon("🏅").pointsReward(25).build(),
        Achievement.builder().id("PARTIDOS_50").name("Jugador Experto")
            .description("50 partidos completados").icon("🥇").pointsReward(75).build(),
        Achievement.builder().id("PARTIDOS_100").name("Centurión")
            .description("100 partidos completados").icon("🏆").pointsReward(150).build(),
        Achievement.builder().id("DEPORTES_3").name("Explorador")
            .description("Jugaste 3 deportes distintos").icon("🌟").pointsReward(30).build(),
        Achievement.builder().id("DEPORTES_5").name("Polideportista")
            .description("Jugaste 5 deportes distintos").icon("⭐").pointsReward(60).build(),
        Achievement.builder().id("PRIMERA_RESENA").name("Crítico Deportivo")
            .description("Escribiste tu primera reseña").icon("✍️").pointsReward(20).build()
    );

    private final UserPointsRepository userPointsRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;

    @PostConstruct
    public void seedAchievements() {
        for (Achievement a : SEED_ACHIEVEMENTS) {
            if (!achievementRepository.existsById(a.getId())) {
                achievementRepository.save(a);
            }
        }
    }

    public void onReservationCreated(User user) {
        UserPoints points = getOrCreate(user);
        long totalReservations = reservationRepository.countByUser(user);
        if (totalReservations == 1) {
            tryUnlock(user, points, "PRIMERA_RESERVA");
            userPointsRepository.save(points);
        }
    }

    public void onReservationAttended(User user) {
        UserPoints points = getOrCreate(user);
        points.setTotalPoints(points.getTotalPoints() + POINTS_PER_ATTENDED);

        long attended = reservationRepository.countByUserAndStatus(user, ReservationStatus.ATTENDED);
        long distinctSports = reservationRepository.countDistinctAttendedSportsByUser(user);

        if (attended >= 10)  tryUnlock(user, points, "PARTIDOS_10");
        if (attended >= 50)  tryUnlock(user, points, "PARTIDOS_50");
        if (attended >= 100) tryUnlock(user, points, "PARTIDOS_100");
        if (distinctSports >= 3) tryUnlock(user, points, "DEPORTES_3");
        if (distinctSports >= 5) tryUnlock(user, points, "DEPORTES_5");

        userPointsRepository.save(points);
    }

    public void onReviewCreated(User user) {
        long reviewCount = reviewRepository.countByUser(user);
        if (reviewCount == 1) {
            UserPoints points = getOrCreate(user);
            tryUnlock(user, points, "PRIMERA_RESENA");
            userPointsRepository.save(points);
        }
    }

    @Transactional(readOnly = true)
    public GamificationProfileResponse getProfile(User user) {
        UserPoints userPoints = userPointsRepository.findByUser(user)
            .orElse(UserPoints.builder().user(user).totalPoints(0).build());

        int total = userPoints.getTotalPoints();
        int level = calculateLevel(total);
        int nextThreshold = level < LEVEL_THRESHOLDS.length ? LEVEL_THRESHOLDS[level] : Integer.MAX_VALUE;
        int toNext = nextThreshold == Integer.MAX_VALUE ? 0 : nextThreshold - total;

        List<UserAchievement> unlocked = userAchievementRepository.findByUserOrderByUnlockedAtDesc(user);
        Set<String> unlockedIds = unlocked.stream()
            .map(ua -> ua.getAchievement().getId())
            .collect(Collectors.toSet());
        Map<String, LocalDateTime> unlockedAt = unlocked.stream()
            .collect(Collectors.toMap(
                ua -> ua.getAchievement().getId(),
                UserAchievement::getUnlockedAt
            ));

        List<AchievementResponse> achievements = SEED_ACHIEVEMENTS.stream()
            .map(a -> new AchievementResponse(
                a.getId(), a.getName(), a.getDescription(), a.getIcon(), a.getPointsReward(),
                unlockedIds.contains(a.getId()),
                unlockedAt.get(a.getId())
            ))
            .toList();

        return new GamificationProfileResponse(
            total, level, LEVEL_NAMES[level - 1], toNext, achievements
        );
    }

    private void tryUnlock(User user, UserPoints points, String achievementId) {
        achievementRepository.findById(achievementId).ifPresent(achievement -> {
            if (!userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
                userAchievementRepository.save(UserAchievement.builder()
                    .user(user)
                    .achievement(achievement)
                    .unlockedAt(LocalDateTime.now())
                    .build());
                points.setTotalPoints(points.getTotalPoints() + achievement.getPointsReward());
                log.info("Achievement unlocked for {}: {} (+{} pts)", user.getEmail(), achievementId, achievement.getPointsReward());
            }
        });
    }

    private UserPoints getOrCreate(User user) {
        return userPointsRepository.findByUser(user)
            .orElse(UserPoints.builder().user(user).totalPoints(0).build());
    }

    private int calculateLevel(int points) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (points >= LEVEL_THRESHOLDS[i]) return i + 1;
        }
        return 1;
    }
}
