package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByOwner(User owner);
    long countByOwner(User owner);

    @Query("SELECT c FROM Court c JOIN FETCH c.owner LEFT JOIN FETCH c.branch WHERE c.owner = :owner AND c.active = true")
    List<Court> findByOwnerAndActiveTrue(@Param("owner") User owner);

    long countByOwnerAndActiveTrue(User owner);

    @Query("SELECT c FROM Court c JOIN FETCH c.owner LEFT JOIN FETCH c.branch WHERE c.active = true")
    List<Court> findByActiveTrue();

    List<Court> findBySportTypeIgnoreCaseAndActiveTrue(String sportType);
    Optional<Court> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    long countByBranchAndActiveTrue(Branch branch);

    @Query("SELECT c FROM Court c JOIN FETCH c.owner LEFT JOIN FETCH c.branch WHERE c.branch IN :branches AND c.active = true")
    List<Court> findByBranchInAndActiveTrue(@Param("branches") List<Branch> branches);

    // Usado por AdminController para el listado global — evita N+1 sobre owner
    @Query("SELECT c FROM Court c JOIN FETCH c.owner")
    List<Court> findAllWithOwner();
}