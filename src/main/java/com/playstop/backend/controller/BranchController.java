package com.playstop.backend.controller;

import com.playstop.backend.dto.request.BranchRequest;
import com.playstop.backend.dto.request.InviteEmployeeRequest;
import com.playstop.backend.dto.response.BranchEmployeeResponse;
import com.playstop.backend.dto.response.BranchResponse;
import com.playstop.backend.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('OWNER') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<BranchResponse>> getMyBranches() {
        return ResponseEntity.ok(branchService.getMyBranches());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BranchResponse> updateBranch(@PathVariable UUID id, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/employees")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> inviteEmployee(@PathVariable UUID id, @Valid @RequestBody InviteEmployeeRequest request) {
        branchService.inviteEmployee(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/employees")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BranchEmployeeResponse>> getEmployees(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getEmployees(id));
    }

    @DeleteMapping("/{id}/employees/{employeeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> removeEmployee(@PathVariable UUID id, @PathVariable UUID employeeId) {
        branchService.removeEmployee(id, employeeId);
        return ResponseEntity.noContent().build();
    }
}
