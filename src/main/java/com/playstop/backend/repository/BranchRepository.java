package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByOwnerAndActiveTrue(User owner);
    long countByOwnerAndActiveTrue(User owner);
}
