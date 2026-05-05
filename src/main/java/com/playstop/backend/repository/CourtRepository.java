package com.playstop.backend.repository;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByOwner(User owner);
    List<Court> findByActiveTrue();
    List<Court> findBySportTypeIgnoreCaseAndActiveTrue(String sportType);
}