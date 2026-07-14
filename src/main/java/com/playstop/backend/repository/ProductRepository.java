package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.Product;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.branch WHERE p.owner = :owner AND p.active = true")
    List<Product> findByOwnerAndActiveTrue(@Param("owner") User owner);

    long countByOwnerAndActiveTrue(User owner);
    long countByBranchAndActiveTrue(Branch branch);
    List<Product> findByBranchInAndActiveTrue(List<Branch> branches);
}
