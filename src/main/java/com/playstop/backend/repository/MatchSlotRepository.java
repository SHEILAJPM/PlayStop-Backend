package com.playstop.backend.repository;

import com.playstop.backend.entity.MatchSlot;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MatchSlotRepository extends JpaRepository<MatchSlot, UUID> {
    @Query("""
        SELECT m FROM MatchSlot m JOIN FETCH m.organizer JOIN FETCH m.court
        WHERE m.open = true AND m.date >= :today
        ORDER BY m.date ASC, m.slotHour ASC
        """)
    List<MatchSlot> findByOpenTrueAndDateGreaterThanEqualOrderByDateAscSlotHourAsc(@Param("today") LocalDate today);

    List<MatchSlot> findByOrganizerOrderByCreatedAtDesc(User organizer);
}
