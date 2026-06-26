package com.playstop.backend.repository;

import com.playstop.backend.entity.User;
import com.playstop.backend.entity.UserPoints;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPointsRepository extends JpaRepository<UserPoints, UUID> {
    Optional<UserPoints> findByUser(User user);
}
