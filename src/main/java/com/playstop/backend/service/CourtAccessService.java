package com.playstop.backend.service;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.BranchEmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Punto único de verdad para "¿este usuario puede gestionar esta cancha?".
 * Reemplaza las comparaciones sueltas `court.getOwner().getId().equals(...)`
 * que antes vivían repetidas en CourtService, ReservationService, ChatService
 * y WsSubscriptionAuthorizationInterceptor.
 */
@Service
@RequiredArgsConstructor
public class CourtAccessService {

    private final BranchEmployeeRepository branchEmployeeRepository;

    public boolean canManageCourt(User user, Court court) {
        if (court.getOwner().getId().equals(user.getId())) {
            return true;
        }
        if (user.getRole() == Role.EMPLOYEE && court.getBranch() != null) {
            return branchEmployeeRepository.existsByBranchAndEmployee(court.getBranch(), user);
        }
        return false;
    }
}
