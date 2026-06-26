package com.playstop.backend.repository;

import com.playstop.backend.entity.MatchSlot;
import com.playstop.backend.entity.MatchSlotParticipant;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchSlotParticipantRepository extends JpaRepository<MatchSlotParticipant, UUID> {
    boolean existsByMatchSlotAndUser(MatchSlot matchSlot, User user);
    List<MatchSlotParticipant> findByMatchSlot(MatchSlot matchSlot);
    List<MatchSlotParticipant> findByUser(User user);
}
