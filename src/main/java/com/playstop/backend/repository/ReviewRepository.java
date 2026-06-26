package com.playstop.backend.repository;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Review;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByCourtOrderByCreatedAtDesc(Court court);

    List<Review> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndCourt(User user, Court court);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.court = :court")
    Optional<Double> findAverageRatingByCourt(Court court);

    long countByCourt(Court court);

    long countByUser(User user);
}
