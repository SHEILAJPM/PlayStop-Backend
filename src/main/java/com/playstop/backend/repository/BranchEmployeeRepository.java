package com.playstop.backend.repository;

import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.BranchEmployee;
import com.playstop.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BranchEmployeeRepository extends JpaRepository<BranchEmployee, UUID> {
    boolean existsByBranchAndEmployee(Branch branch, User employee);

    @Query("SELECT be FROM BranchEmployee be JOIN FETCH be.employee WHERE be.branch = :branch")
    List<BranchEmployee> findByBranch(@Param("branch") Branch branch);

    @Query("SELECT be FROM BranchEmployee be JOIN FETCH be.branch WHERE be.employee = :employee")
    List<BranchEmployee> findByEmployee(@Param("employee") User employee);

    void deleteByBranchAndEmployee(Branch branch, User employee);
}
