package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.request.PayoutRequestDto;
import com.playstop.backend.dto.response.PayoutResponse;
import com.playstop.backend.entity.PayoutRequest;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.PayoutStatus;
import com.playstop.backend.repository.PayoutRequestRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PayoutService {

    private static final List<PayoutStatus> RESERVED_STATUSES = List.of(PayoutStatus.PENDING, PayoutStatus.PAID);

    private final PayoutRequestRepository payoutRequestRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public BigDecimal getAvailableBalance(User owner) {
        Object[] stats = reservationRepository.findTotalStatsByOwner(owner);
        BigDecimal totalRevenue = stats != null && stats[1] != null ? (BigDecimal) stats[1] : BigDecimal.ZERO;
        BigDecimal alreadyReservedOrPaid = payoutRequestRepository.sumAmountByOwnerAndStatusIn(owner, RESERVED_STATUSES);
        BigDecimal available = totalRevenue.subtract(alreadyReservedOrPaid);
        return available.max(BigDecimal.ZERO);
    }

    public PayoutResponse createPayoutRequest(PayoutRequestDto dto) {
        User owner = getCurrentUser();

        if ("YAPE_PLIN".equals(dto.getMethod()) && (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de celular es obligatorio para Yape/Plin");
        }
        if ("BANK".equals(dto.getMethod()) && (dto.getBankName() == null || dto.getBankName().isBlank()
                || dto.getAccountNumber() == null || dto.getAccountNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El banco y número de cuenta son obligatorios");
        }

        BigDecimal available = getAvailableBalance(owner);
        if (dto.getAmount().compareTo(available) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El monto solicitado (S/ " + dto.getAmount() + ") supera tu saldo disponible (S/ " + available + ")");
        }

        PayoutRequest request = PayoutRequest.builder()
                .owner(owner)
                .amount(dto.getAmount())
                .method(dto.getMethod())
                .holderName(dto.getHolderName())
                .phoneNumber(dto.getPhoneNumber())
                .bankName(dto.getBankName())
                .accountType(dto.getAccountType())
                .accountNumber(dto.getAccountNumber())
                .status(PayoutStatus.PENDING)
                .build();

        return toResponse(payoutRequestRepository.save(request));
    }

    public List<PayoutResponse> getMyPayoutRequests() {
        User owner = getCurrentUser();
        return payoutRequestRepository.findByOwnerOrderByRequestedAtDesc(owner)
                .stream().map(this::toResponse).toList();
    }

    public List<PayoutResponse> getAllPayoutRequests(String status) {
        List<PayoutRequest> requests = (status == null || status.isBlank())
                ? payoutRequestRepository.findAllByOrderByRequestedAtDesc()
                : payoutRequestRepository.findByStatusOrderByRequestedAtDesc(PayoutStatus.valueOf(status.toUpperCase()));
        return requests.stream().map(this::toAdminResponse).toList();
    }

    public PayoutResponse markAsPaid(UUID id) {
        PayoutRequest request = payoutRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (request.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta solicitud ya fue resuelta");
        }

        request.setStatus(PayoutStatus.PAID);
        request.setResolvedAt(java.time.LocalDateTime.now());
        PayoutRequest saved = payoutRequestRepository.save(request);

        emailService.sendPayoutPaid(request.getOwner().getEmail(), request.getOwner().getName(), request.getAmount());

        return toAdminResponse(saved);
    }

    public PayoutResponse reject(UUID id, String reason) {
        PayoutRequest request = payoutRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (request.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta solicitud ya fue resuelta");
        }

        request.setStatus(PayoutStatus.REJECTED);
        request.setAdminNotes(reason);
        request.setResolvedAt(java.time.LocalDateTime.now());
        PayoutRequest saved = payoutRequestRepository.save(request);

        emailService.sendPayoutRejected(request.getOwner().getEmail(), request.getOwner().getName(), request.getAmount(), reason);

        return toAdminResponse(saved);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private PayoutResponse toResponse(PayoutRequest r) {
        return PayoutResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .method(r.getMethod())
                .holderName(r.getHolderName())
                .phoneNumber(r.getPhoneNumber())
                .bankName(r.getBankName())
                .accountType(r.getAccountType())
                .accountNumber(r.getAccountNumber())
                .status(r.getStatus())
                .adminNotes(r.getAdminNotes())
                .requestedAt(r.getRequestedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }

    private PayoutResponse toAdminResponse(PayoutRequest r) {
        PayoutResponse response = toResponse(r);
        response.setOwnerName(r.getOwner().getName());
        response.setOwnerEmail(r.getOwner().getEmail());
        return response;
    }
}
