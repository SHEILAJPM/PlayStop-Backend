package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.request.CourtRequest;
import com.playstop.backend.dto.response.CourtResponse;
import com.playstop.backend.dto.response.SlotResponse;
import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.BranchEmployee;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.enums.Role;
import com.playstop.backend.enums.SubscriptionPlan;
import com.playstop.backend.repository.BranchEmployeeRepository;
import com.playstop.backend.repository.BranchRepository;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.ReviewRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BranchRepository branchRepository;
    private final BranchEmployeeRepository branchEmployeeRepository;
    private final CourtAccessService courtAccessService;

    public CourtResponse getCourtBySlug(String slug) {
        Court court = courtRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));
        return toResponse(court);
    }

    // Obtener todas las canchas activas
    public List<CourtResponse> getAllCourts() {
        return toResponseList(courtRepository.findByActiveTrue());
    }

    // Obtener cancha por ID
    public CourtResponse getCourtById(UUID id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));
        return toResponse(court);
    }

    // Crear cancha (solo OWNER)
    private static final int PLAN_BASICO_MAX_COURTS = 2;

    public CourtResponse createCourt(CourtRequest request) {
        User currentUser = getCurrentUser();
        Branch branch = resolveBranchForWrite(currentUser, request.getBranchId());
        User owner = branch != null ? branch.getOwner() : currentUser;

        if (owner.getPlan() == SubscriptionPlan.BASICO
                && courtRepository.countByOwnerAndActiveTrue(owner) >= PLAN_BASICO_MAX_COURTS) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                "El Plan Básico permite hasta " + PLAN_BASICO_MAX_COURTS + " canchas. Actualiza a Pro para agregar más.");
        }

        Court court = Court.builder()
                .name(request.getName())
                .sportType(request.getSportType())
                .pricePerHour(request.getPricePerHour())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .city(request.getCity())
                .district(request.getDistrict())
                .active(true)
                .owner(owner)
                .branch(branch)
                .slug(generateUniqueSlug(request.getDistrict(), request.getName(), request.getSportType(), null))
                .build();
        return toResponse(courtRepository.save(court));
    }

    // Editar cancha (OWNER dueño de la cancha, o EMPLOYEE de su sucursal)
    public CourtResponse updateCourt(UUID id, CourtRequest request) {
        User currentUser = getCurrentUser();
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        if (!courtAccessService.canManageCourt(currentUser, court)) {
            throw new BusinessException("No tienes permiso para editar esta cancha");
        }

        court.setName(request.getName());
        court.setSportType(request.getSportType());
        court.setPricePerHour(request.getPricePerHour());
        court.setAddress(request.getAddress());
        court.setLatitude(request.getLatitude());
        court.setLongitude(request.getLongitude());
        court.setImageUrl(request.getImageUrl());
        court.setCity(request.getCity());
        court.setDistrict(request.getDistrict());
        court.setBranch(resolveBranchForWrite(currentUser, request.getBranchId()));
        court.setSlug(generateUniqueSlug(request.getDistrict(), request.getName(), request.getSportType(), court.getId()));

        return toResponse(courtRepository.save(court));
    }

    // Eliminar cancha (OWNER dueño de la cancha, o EMPLOYEE de su sucursal)
    public void deleteCourt(UUID id) {
        User currentUser = getCurrentUser();
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        if (!courtAccessService.canManageCourt(currentUser, court)) {
            throw new BusinessException("No tienes permiso para eliminar esta cancha");
        }

        court.setActive(false);
        courtRepository.save(court);
    }

    // Ver mis canchas (OWNER: todas las suyas; EMPLOYEE: solo las de su(s) sucursal(es))
    public List<CourtResponse> getMyCourts() {
        User currentUser = getCurrentUser();

        List<Court> courts;
        if (currentUser.getRole() == Role.EMPLOYEE) {
            List<Branch> branches = branchEmployeeRepository.findByEmployee(currentUser).stream()
                    .map(BranchEmployee::getBranch)
                    .distinct()
                    .toList();
            courts = branches.isEmpty() ? List.of() : courtRepository.findByBranchInAndActiveTrue(branches);
        } else {
            courts = courtRepository.findByOwnerAndActiveTrue(currentUser);
        }

        return toResponseList(courts);
    }

    // Resuelve y valida la sucursal opcional de una cancha: un EMPLOYEE debe
    // pertenecer a ella, un OWNER debe ser su dueño. null si no se especifica
    // (cancha sin sucursal, ej. Owners fuera del plan Enterprise).
    private Branch resolveBranchForWrite(User currentUser, UUID branchId) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (branchId == null) {
                throw new BusinessException("Selecciona la sucursal para esta cancha");
            }
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new BusinessException("Sucursal no encontrada"));
            if (!branchEmployeeRepository.existsByBranchAndEmployee(branch, currentUser)) {
                throw new BusinessException("No tienes permiso sobre esta sucursal");
            }
            return branch;
        }

        if (branchId == null) return null;
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("Sucursal no encontrada"));
        if (!branch.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException("No tienes permiso sobre esta sucursal");
        }
        return branch;
    }

    // Ver slots disponibles de una cancha en una fecha
    public List<SlotResponse> getAvailableSlots(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        // Cada reserva ocupa [slotHour, slotHour + durationHours), no solo su hora de inicio
        List<Object[]> occupiedRanges = reservationRepository
                .findOccupiedRanges(court, date, ReservationStatus.CANCELLED);

        Set<Integer> occupiedHours = new HashSet<>();
        for (Object[] range : occupiedRanges) {
            int start = (int) range[0];
            int duration = (int) range[1];
            for (int h = start; h < start + duration; h++) occupiedHours.add(h);
        }

        List<SlotResponse> slots = new ArrayList<>();
        for (int hour = 6; hour <= 23; hour++) {
            boolean available = !occupiedHours.contains(hour);
            String label = String.format("%02d:00 - %02d:00", hour, hour + 1);
            slots.add(new SlotResponse(hour, label, available));
        }
        return slots;
    }

    private String generateUniqueSlug(String district, String name, String sportType, UUID excludeId) {
        String base = toSlug((district != null ? district + "-" : "") + name + "-" + sportType);
        String slug = base;
        int i = 2;
        while (true) {
            boolean taken = excludeId != null
                    ? courtRepository.existsBySlugAndIdNot(slug, excludeId)
                    : courtRepository.existsBySlug(slug);
            if (!taken) return slug;
            slug = base + "-" + i++;
        }
    }

    private static String toSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private CourtResponse toResponse(Court court) {
        Double avgRating = reviewRepository.findAverageRatingByCourt(court).orElse(null);
        long reviewCount = reviewRepository.countByCourt(court);
        return toResponse(court, avgRating, reviewCount);
    }

    // Arma la respuesta de varias canchas trayendo el promedio/total de
    // reseñas en una sola consulta agregada, en vez de 2 consultas por cancha.
    private List<CourtResponse> toResponseList(List<Court> courts) {
        if (courts.isEmpty()) return List.of();

        Map<UUID, Object[]> statsByCourtId = reviewRepository.findRatingStatsByCourts(courts).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> row));

        return courts.stream()
                .map(court -> {
                    Object[] stats = statsByCourtId.get(court.getId());
                    Double avgRating = stats != null ? (Double) stats[1] : null;
                    long reviewCount = stats != null ? (Long) stats[2] : 0L;
                    return toResponse(court, avgRating, reviewCount);
                })
                .toList();
    }

    private CourtResponse toResponse(Court court, Double avgRating, long reviewCount) {
        return CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .sportType(court.getSportType())
                .pricePerHour(court.getPricePerHour())
                .address(court.getAddress())
                .latitude(court.getLatitude())
                .longitude(court.getLongitude())
                .imageUrl(court.getImageUrl())
                .active(court.isActive())
                .ownerName(court.getOwner().getName())
                .ownerEmail(court.getOwner().getEmail())
                .city(court.getCity())
                .district(court.getDistrict())
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null)
                .reviewCount(reviewCount)
                .slug(court.getSlug())
                .branchId(court.getBranch() != null ? court.getBranch().getId() : null)
                .branchName(court.getBranch() != null ? court.getBranch().getName() : null)
                .build();
    }
}