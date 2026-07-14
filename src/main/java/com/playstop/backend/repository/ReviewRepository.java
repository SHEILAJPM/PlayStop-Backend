package com.playstop.backend.repository;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Review;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.court = :court ORDER BY r.createdAt DESC")
    List<Review> findByCourtOrderByCreatedAtDesc(@Param("court") Court court);

    List<Review> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndCourt(User user, Court court);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.court = :court")
    Optional<Double> findAverageRatingByCourt(Court court);

    long countByCourt(Court court);

    // Trae promedio + total en una sola consulta para varias canchas a la
    // vez (usado al listar canchas, para no hacer 2 consultas por fila).
    @Query("SELECT r.court.id, AVG(r.rating), COUNT(r) FROM Review r WHERE r.court IN :courts GROUP BY r.court.id")
    List<Object[]> findRatingStatsByCourts(@Param("courts") List<Court> courts);

    long countByUser(User user);
}
