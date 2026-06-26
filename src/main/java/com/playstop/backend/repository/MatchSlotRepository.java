package com.playstop.backend.repository;

import com.playstop.backend.entity.MatchSlot;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MatchSlotRepository extends JpaRepository<MatchSlot, UUID> {
    List<MatchSlot> findByOpenTrueAndDateGreaterThanEqualOrderByDateAscSlotHourAsc(LocalDate today);
    List<MatchSlot> findByOrganizerOrderByCreatedAtDesc(User organizer);
}
