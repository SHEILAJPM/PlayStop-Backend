package com.playstop.backend.service;

import com.playstop.backend.dto.request.BranchRequest;
import com.playstop.backend.dto.request.InviteEmployeeRequest;
import com.playstop.backend.dto.response.BranchEmployeeResponse;
import com.playstop.backend.dto.response.BranchResponse;
import com.playstop.backend.dto.response.InvitationInfoResponse;
import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.BranchEmployee;
import com.playstop.backend.entity.BranchInvitation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import com.playstop.backend.enums.SubscriptionPlan;
import com.playstop.backend.exception.BusinessException;
import com.playstop.backend.repository.BranchEmployeeRepository;
import com.playstop.backend.repository.BranchInvitationRepository;
import com.playstop.backend.repository.BranchRepository;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BranchService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BranchRepository branchRepository;
    private final BranchEmployeeRepository branchEmployeeRepository;
    private final BranchInvitationRepository branchInvitationRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ── Sucursales ──────────────────────────────────────────────────────────

    public BranchResponse createBranch(BranchRequest request) {
        User owner = getCurrentUser();

        if (owner.getPlan() != SubscriptionPlan.ENTERPRISE) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Las sucursales solo están disponibles en el Plan Enterprise.");
        }

        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .district(request.getDistrict())
                .owner(owner)
                .build();

        return toResponse(branchRepository.save(branch));
    }

    public List<BranchResponse> getMyBranches() {
        User user = getCurrentUser();

        List<Branch> branches;
        if (user.getRole() == Role.EMPLOYEE) {
            branches = branchEmployeeRepository.findByEmployee(user).stream()
                    .map(BranchEmployee::getBranch)
                    .distinct()
                    .toList();
        } else {
            branches = branchRepository.findByOwnerAndActiveTrue(user);
        }

        return branches.stream().map(this::toResponse).toList();
    }

    public BranchResponse updateBranch(UUID id, BranchRequest request) {
        User owner = getCurrentUser();
        Branch branch = getOwnedBranch(id, owner);

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setDistrict(request.getDistrict());

        return toResponse(branchRepository.save(branch));
    }

    public void deleteBranch(UUID id) {
        User owner = getCurrentUser();
        Branch branch = getOwnedBranch(id, owner);

        if (courtRepository.countByBranchAndActiveTrue(branch) > 0) {
            throw new BusinessException("Reasigna o desactiva las canchas de esta sucursal antes de eliminarla");
        }

        branch.setActive(false);
        branchRepository.save(branch);
    }

    // ── Empleados ────────────────────────────────────────────────────────────

    public void inviteEmployee(UUID branchId, InviteEmployeeRequest request) {
        User owner = getCurrentUser();
        Branch branch = getOwnedBranch(branchId, owner);

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(u -> u.getRole() != Role.EMPLOYEE)
                .isPresent()) {
            throw new BusinessException("Ese email ya pertenece a una cuenta existente de PlayStop");
        }

        String token = generateToken();
        BranchInvitation invitation = BranchInvitation.builder()
                .branch(branch)
                .email(normalizedEmail)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();
        branchInvitationRepository.save(invitation);

        emailService.sendBranchInvitation(normalizedEmail, owner.getName(), branch.getName(), token);
    }

    public List<BranchEmployeeResponse> getEmployees(UUID branchId) {
        User owner = getCurrentUser();
        Branch branch = getOwnedBranch(branchId, owner);

        return branchEmployeeRepository.findByBranch(branch).stream()
                .map(be -> BranchEmployeeResponse.builder()
                        .id(be.getId())
                        .employeeId(be.getEmployee().getId())
                        .name(be.getEmployee().getName())
                        .email(be.getEmployee().getEmail())
                        .phone(be.getEmployee().getPhone())
                        .build())
                .toList();
    }

    public void removeEmployee(UUID branchId, UUID employeeId) {
        User owner = getCurrentUser();
        Branch branch = getOwnedBranch(branchId, owner);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Empleado no encontrado"));

        branchEmployeeRepository.deleteByBranchAndEmployee(branch, employee);
    }

    // ── Invitación (usado por AuthController, sin autenticación) ────────────

    public InvitationInfoResponse getInvitationInfo(String token) {
        BranchInvitation invitation = branchInvitationRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new BusinessException("Invitación inválida o ya utilizada"));

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            throw new BusinessException("La invitación ha expirado");
        }

        return InvitationInfoResponse.builder()
                .email(invitation.getEmail())
                .branchName(invitation.getBranch().getName())
                .ownerName(invitation.getBranch().getOwner().getName())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Branch getOwnedBranch(UUID id, User owner) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Sucursal no encontrada"));

        if (!branch.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException("No tienes permiso sobre esta sucursal");
        }
        return branch;
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private BranchResponse toResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .city(branch.getCity())
                .district(branch.getDistrict())
                .active(branch.isActive())
                .courtCount(courtRepository.countByBranchAndActiveTrue(branch))
                .employeeCount(branchEmployeeRepository.findByBranch(branch).size())
                .build();
    }
}
