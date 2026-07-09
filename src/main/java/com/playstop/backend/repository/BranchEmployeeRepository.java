package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.BranchEmployee;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchEmployeeRepository extends JpaRepository<BranchEmployee, UUID> {
    boolean existsByBranchAndEmployee(Branch branch, User employee);
    List<BranchEmployee> findByBranch(Branch branch);
    List<BranchEmployee> findByEmployee(User employee);
    void deleteByBranchAndEmployee(Branch branch, User employee);
}
