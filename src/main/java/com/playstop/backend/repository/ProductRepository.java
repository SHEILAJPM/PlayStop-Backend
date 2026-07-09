package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.Product;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByOwnerAndActiveTrue(User owner);
    long countByOwnerAndActiveTrue(User owner);
    long countByBranchAndActiveTrue(Branch branch);
    List<Product> findByBranchInAndActiveTrue(List<Branch> branches);
}
