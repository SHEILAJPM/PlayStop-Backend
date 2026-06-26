package com.playstop.backend.repository;

import com.playstop.backend.entity.Achievement;
import com.playstop.backend.entity.User;
import com.playstop.backend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    List<UserAchievement> findByUserOrderByUnlockedAtDesc(User user);
    boolean existsByUserAndAchievement(User user, Achievement achievement);
}
