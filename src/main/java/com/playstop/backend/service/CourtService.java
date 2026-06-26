package com.playstop.backend.service;

import com.playstop.backend.dto.request.CourtRequest;
import com.playstop.backend.dto.response.CourtResponse;
import com.playstop.backend.dto.response.SlotResponse;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.ReviewRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public CourtResponse getCourtBySlug(String slug) {
        Court court = courtRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
        return toResponse(court);
    }

    // Obtener todas las canchas activas
    public List<CourtResponse> getAllCourts() {
        return courtRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Obtener cancha por ID
    public CourtResponse getCourtById(UUID id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
        return toResponse(court);
    }

    // Crear cancha (solo OWNER)
    public CourtResponse createCourt(CourtRequest request) {
        User owner = getCurrentUser();
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
                .slug(generateUniqueSlug(request.getDistrict(), request.getName(), request.getSportType(), null))
                .build();
        return toResponse(courtRepository.save(court));
    }

    // Editar cancha (solo OWNER dueño de la cancha)
    public CourtResponse updateCourt(UUID id, CourtRequest request) {
        User owner = getCurrentUser();
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        if (!court.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("No tienes permiso para editar esta cancha");
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
        court.setSlug(generateUniqueSlug(request.getDistrict(), request.getName(), request.getSportType(), court.getId()));

        return toResponse(courtRepository.save(court));
    }

    // Eliminar cancha
    public void deleteCourt(UUID id) {
        User owner = getCurrentUser();
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        if (!court.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta cancha");
        }

        court.setActive(false);
        courtRepository.save(court);
    }

    // Ver mis canchas (OWNER)
    public List<CourtResponse> getMyCourts() {
        User owner = getCurrentUser();
        return courtRepository.findByOwner(owner)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Ver slots disponibles de una cancha en una fecha
    public List<SlotResponse> getAvailableSlots(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        List<Integer> occupiedSlots = reservationRepository
                .findOccupiedSlots(court, date, ReservationStatus.CANCELLED);

        List<SlotResponse> slots = new ArrayList<>();
        for (int hour = 6; hour <= 23; hour++) {
            boolean available = !occupiedSlots.contains(hour);
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private CourtResponse toResponse(Court court) {
        Double avgRating = reviewRepository.findAverageRatingByCourt(court).orElse(null);
        long reviewCount = reviewRepository.countByCourt(court);
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
                .build();
    }
}