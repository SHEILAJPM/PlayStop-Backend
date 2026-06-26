package com.playstop.backend.repository;

import com.playstop.backend.entity.ReservationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationMessageRepository extends JpaRepository<ReservationMessage, UUID> {

    List<ReservationMessage> findByReservationIdOrderBySentAtAsc(UUID reservationId);

    long countByReservationIdAndSenderIdAndSentAtAfter(
        UUID reservationId, UUID senderId, java.time.LocalDateTime after
    );
}
