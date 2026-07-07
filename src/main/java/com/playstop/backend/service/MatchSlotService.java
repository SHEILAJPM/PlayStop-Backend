package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.request.MatchSlotRequest;
import com.playstop.backend.dto.response.MatchSlotResponse;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.MatchSlot;
import com.playstop.backend.entity.MatchSlotParticipant;
import com.playstop.backend.entity.User;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.MatchSlotParticipantRepository;
import com.playstop.backend.repository.MatchSlotRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchSlotService {

    private final MatchSlotRepository matchSlotRepository;
    private final MatchSlotParticipantRepository participantRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    public List<MatchSlotResponse> getOpenMatches() {
        return matchSlotRepository
                .findByOpenTrueAndDateGreaterThanEqualOrderByDateAscSlotHourAsc(LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    public MatchSlotResponse createMatch(MatchSlotRequest request) {
        User organizer = getCurrentUser();
        Court court = courtRepository.findById(request.courtId())
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        MatchSlot slot = MatchSlot.builder()
                .court(court)
                .organizer(organizer)
                .date(request.date())
                .slotHour(request.slotHour())
                .totalPlayers(request.totalPlayers())
                .currentPlayers(1)
                .pricePerPlayer(request.pricePerPlayer())
                .description(request.description())
                .open(true)
                .build();

        MatchSlot saved = matchSlotRepository.save(slot);

        participantRepository.save(MatchSlotParticipant.builder()
                .matchSlot(saved)
                .user(organizer)
                .build());

        return toResponse(saved);
    }

    public MatchSlotResponse joinMatch(UUID matchId) {
        User user = getCurrentUser();
        MatchSlot slot = matchSlotRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException("Partido no encontrado"));

        if (!slot.isOpen()) throw new BusinessException("Este partido ya está lleno o cerrado");
        if (participantRepository.existsByMatchSlotAndUser(slot, user))
            throw new BusinessException("Ya te uniste a este partido");

        participantRepository.save(MatchSlotParticipant.builder()
                .matchSlot(slot).user(user).build());

        slot.setCurrentPlayers(slot.getCurrentPlayers() + 1);
        if (slot.getCurrentPlayers() >= slot.getTotalPlayers()) slot.setOpen(false);

        return toResponse(matchSlotRepository.save(slot));
    }

    public void cancelMatch(UUID matchId) {
        User user = getCurrentUser();
        MatchSlot slot = matchSlotRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException("Partido no encontrado"));
        if (!slot.getOrganizer().getId().equals(user.getId()))
            throw new BusinessException("Solo el organizador puede cancelar el partido");
        slot.setOpen(false);
        matchSlotRepository.save(slot);
    }

    private MatchSlotResponse toResponse(MatchSlot s) {
        String avatar = s.getOrganizer().getProfileImageUrl() != null
                ? s.getOrganizer().getProfileImageUrl()
                : "https://ui-avatars.com/api/?name=" +
                  s.getOrganizer().getName().replace(" ", "+") + "&background=0f172a&color=fff&size=80";
        return new MatchSlotResponse(
                s.getId(), s.getCourt().getId(), s.getCourt().getName(),
                s.getCourt().getImageUrl(), s.getCourt().getDistrict(),
                s.getCourt().getSportType(), s.getOrganizer().getName(), avatar,
                s.getDate(), s.getSlotHour(), s.getTotalPlayers(), s.getCurrentPlayers(),
                s.getTotalPlayers() - s.getCurrentPlayers(), s.getPricePerPlayer(),
                s.getDescription(), s.isOpen(), s.getCreatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }
}
