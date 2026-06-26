package com.playstop.backend.repository;

import com.playstop.backend.entity.Referral;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    List<Referral> findByReferrerOrderByCreatedAtDesc(User referrer);
    boolean existsByReferred(User referred);
    long countByReferrer(User referrer);
}
