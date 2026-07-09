package com.playstop.backend.repository;

import com.playstop.backend.entity.BranchInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchInvitationRepository extends JpaRepository<BranchInvitation, UUID> {
    Optional<BranchInvitation> findByTokenAndUsedFalse(String token);
}
