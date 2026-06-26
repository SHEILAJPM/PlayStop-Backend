package com.playstop.backend.repository;

import com.playstop.backend.entity.PasswordResetToken;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByCodeAndUsedFalse(String code);
    void deleteByUser(User user);
}