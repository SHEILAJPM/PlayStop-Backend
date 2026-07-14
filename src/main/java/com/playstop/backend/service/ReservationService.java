package com.playstop.backend.service;

import com.playstop.backend.exception.BusinessException;

import com.playstop.backend.dto.request.ReservationRequest;
import com.playstop.backend.dto.response.ReservationResponse;
import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.BranchEmployee;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.BranchEmployeeRepository;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final QrService qrService;
    private final WhatsAppService whatsAppService;
    private final CourtAccessService courtAccessService;
    private final BranchEmployeeRepository branchEmployeeRepository;

    @Lazy
    @Autowired
    private GamificationService gamificationService;

    public ReservationResponse createReservation(ReservationRequest request) {
        User user = getCurrentUser();

        // ── Bloqueo de cuenta ────────────────────────────────────────────────
        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Tu cuenta está deshabilitada. No puedes realizar reservas.");
        }
        if (user.isChatPermanentlyBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Tu cuenta está bloqueada permanentemente y no puede realizar nuevas reservas.");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        if (!court.isActive()) {
            throw new BusinessException("La cancha no está disponible");
        }

        int durationHours = request.getDurationHours() != null ? request.getDurationHours() : 1;
        int startHour = request.getSlotHour();
        int endHour = startHour + durationHours;

        if (endHour > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La reserva no puede extenderse más allá de las 12 AM");
        }

        boolean overlapping = reservationRepository.existsOverlapping(
                court, request.getDate(), startHour, endHour, ReservationStatus.CANCELLED
        );

        if (overlapping) {
            throw new BusinessException("Ese horario ya está reservado");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .court(court)
                .date(request.getDate())
                .slotHour(startHour)
                .durationHours(durationHours)
                .totalAmount(court.getPricePerHour().multiply(java.math.BigDecimal.valueOf(durationHours)))
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // La reserva queda PENDING hasta que el pago se confirme (ver
        // confirmReservationPayment, invocado desde el webhook de Stripe).
        // No se envían notificaciones ni se otorgan puntos todavía.

        return toResponse(saved);
    }

    /**
     * Invocado desde el webhook de Stripe cuando el pago se completa. Marca
     * la reserva como CONFIRMED (transaccion corta) y devuelve los datos
     * necesarios para las notificaciones, que el llamador debe enviar
     * llamando aparte a sendConfirmationNotifications — asi la conexion a BD
     * no queda retenida durante las llamadas HTTP lentas (email/WhatsApp).
     */
    public ConfirmationNotificationData confirmReservationPayment(UUID reservationId) {
        Reservation saved = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (saved.getStatus() != ReservationStatus.PENDING) {
            log.warn("Se intentó confirmar el pago de una reserva que no está PENDING: {} ({})",
                    reservationId, saved.getStatus());
            return null;
        }

        saved.setStatus(ReservationStatus.CONFIRMED);
        saved = reservationRepository.save(saved);

        User user = saved.getUser();
        Court court = saved.getCourt();
        gamificationService.onReservationCreated(user);

        String slot = String.format("%02d:00 - %02d:00", saved.getSlotHour(), saved.getSlotHour() + saved.getDurationHours());

        return new ConfirmationNotificationData(
            saved.getId(), user.getEmail(), user.getName(), user.getPhone(),
            court.getName(), court.getOwner().getEmail(), court.getOwner().getName(),
            saved.getDate().toString(), slot, saved.getTotalAmount()
        );
    }

    /**
     * QR + emails + WhatsApp de confirmacion. Propagation.NOT_SUPPORTED
     * asegura que esto corre sin transaccion/conexion a BD abierta, para no
     * agotar el pool (5 conexiones) mientras esperamos a Brevo/Meta.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendConfirmationNotifications(ConfirmationNotificationData data) {
        // 1. Generar QR y enviarlo al jugador en el email de confirmación
        try {
            String qrContent = String.format(
                "PLAYSTOP|ID:%s|CANCHA:%s|FECHA:%s|HORA:%s|CLIENTE:%s|MONTO:S/ %.2f",
                data.reservationId(), data.courtName(), data.date(), data.slot(),
                data.userName(), data.totalAmount()
            );
            byte[] qrBytes = qrService.generateQr(qrContent);
            emailService.sendReservationConfirmationWithQr(
                data.userEmail(), data.userName(), data.courtName(),
                data.date(), data.slot(), data.reservationId().toString(), qrBytes
            );
            log.info("Email de confirmación con QR enviado al jugador: {}", data.userEmail());
        } catch (Exception ex) {
            log.error("Error al generar QR para reserva {}, enviando email sin QR: {}", data.reservationId(), ex.getMessage());
            emailService.sendReservationConfirmation(
                data.userEmail(), data.userName(), data.courtName(),
                data.date(), data.slot()
            );
        }

        // 2. Enviar términos y condiciones al jugador (email separado)
        emailService.sendTermsAndConditionsToPlayer(
            data.userEmail(), data.userName(), data.courtName(),
            data.date(), data.slot(), data.reservationId().toString()
        );
        log.info("Email de términos y condiciones enviado al jugador: {}", data.userEmail());

        // 3. WhatsApp (no-op si Meta Cloud API no está configurado)
        whatsAppService.sendReservationConfirmation(
            data.userPhone(), data.userName(), data.courtName(),
            data.date(), data.slot(),
            data.totalAmount().toPlainString()
        );

        // 4. Notificar al propietario sobre la nueva reserva
        emailService.sendNewReservationNotificationToOwner(
            data.ownerEmail(), data.ownerName(),
            data.userName(), data.userEmail(),
            data.courtName(), data.date(), data.slot(),
            data.reservationId().toString(), data.totalAmount().doubleValue()
        );
    }

    public record ConfirmationNotificationData(
        UUID reservationId, String userEmail, String userName, String userPhone,
        String courtName, String ownerEmail, String ownerName,
        String date, String slot, java.math.BigDecimal totalAmount
    ) {}

    /**
     * Invocado desde el webhook de Stripe cuando la sesión de checkout expira
     * sin completarse (el usuario abandonó el pago). Libera el horario.
     */
    public void cancelExpiredReservation(UUID reservationId) {
        reservationRepository.findById(reservationId).ifPresent(r -> {
            if (r.getStatus() == ReservationStatus.PENDING) {
                r.setStatus(ReservationStatus.CANCELLED);
                reservationRepository.save(r);
                log.info("Reserva {} cancelada por expiración de la sesión de pago", reservationId);
            }
        });
    }

    public List<ReservationResponse> getMyReservations() {
        User user = getCurrentUser();
        return reservationRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationResponse> getReservationsByCourt(UUID courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new BusinessException("Cancha no encontrada"));

        User currentUser = getCurrentUser();
        if (!courtAccessService.canManageCourt(currentUser, court)) {
            throw new BusinessException("No tienes permiso para ver estas reservas");
        }

        return reservationRepository.findByCourt(court)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Reservas de todas las canchas administrables por el usuario actual
    // (dueño o empleado), en una sola consulta en vez de una por cancha.
    public List<ReservationResponse> getReservationsForCurrentOwner() {
        User currentUser = getCurrentUser();
        List<Court> courts = currentUser.getRole() == Role.EMPLOYEE
                ? resolveEmployeeCourts(currentUser)
                : courtRepository.findByOwnerAndActiveTrue(currentUser);

        if (courts.isEmpty()) return List.of();
        return reservationRepository.findByCourtIn(courts)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Court> resolveEmployeeCourts(User employee) {
        List<Branch> branches = branchEmployeeRepository.findByEmployee(employee).stream()
                .map(BranchEmployee::getBranch)
                .distinct()
                .toList();
        return branches.isEmpty() ? List.of() : courtRepository.findByBranchInAndActiveTrue(branches);
    }

    public ReservationResponse cancelReservation(UUID reservationId) {
        User user = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("La reserva ya está cancelada");
        }

        // Una reserva PENDING nunca se pagó ni se confirmó — no hay nada que
        // proteger con la regla de 24h, y bloquearla dejaría reservas
        // abandonadas (pago nunca completado) canceladas para siempre si su
        // fecha ya pasó.
        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        if (wasConfirmed) {
            LocalDateTime reservationDateTime = reservation.getDate()
                    .atTime(reservation.getSlotHour(), 0);

            if (LocalDateTime.now().isAfter(reservationDateTime.minusHours(24))) {
                throw new BusinessException("Solo puedes cancelar hasta 24 horas antes de la reserva");
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        // El email de cancelación solo aplica si la reserva llegó a confirmarse
        // (si seguía PENDING, el propietario nunca fue notificado de ella).
        if (wasConfirmed) {
            emailService.sendReservationCancellation(
                user.getEmail(),
                user.getName(),
                reservation.getCourt().getName(),
                reservation.getDate().toString(),
                String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + reservation.getDurationHours())
            );
        }

        return toResponse(saved);
    }

    public ReservationResponse cancelReservationByOwner(UUID reservationId) {
        User currentUser = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!courtAccessService.canManageCourt(currentUser, reservation.getCourt())) {
            throw new BusinessException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("La reserva ya está cancelada");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        emailService.sendReservationCancellation(
            reservation.getUser().getEmail(),
            reservation.getUser().getName(),
            reservation.getCourt().getName(),
            reservation.getDate().toString(),
            String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + reservation.getDurationHours())
        );

        return toResponse(saved);
    }

    public ReservationResponse getReservationById(UUID id) {
        User user = getCurrentUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("No tienes permiso para ver esta reserva");
        }

        return toResponse(reservation);
    }

    public byte[] getReservationQr(UUID reservationId) {
        User user = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("No tienes permiso");
        }

        String slot = String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + reservation.getDurationHours());
        String qrContent = String.format(
            "PLAYSTOP|ID:%s|CANCHA:%s|FECHA:%s|HORA:%s|CLIENTE:%s|MONTO:S/ %.2f",
            reservation.getId(), reservation.getCourt().getName(),
            reservation.getDate(), slot,
            reservation.getUser().getName(), reservation.getTotalAmount()
        );
        try {
            return qrService.generateQr(qrContent);
        } catch (Exception e) {
            throw new BusinessException("Error generando QR: " + e.getMessage());
        }
    }

    public ReservationResponse verifyReservation(UUID reservationId) {
        User currentUser = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!courtAccessService.canManageCourt(currentUser, reservation.getCourt())) {
            throw new BusinessException("Esta reserva no pertenece a ninguna de tus canchas");
        }

        return toResponse(reservation);
    }

    public ReservationResponse confirmAttendance(UUID reservationId) {
        User currentUser = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada"));

        if (!courtAccessService.canManageCourt(currentUser, reservation.getCourt())) {
            throw new BusinessException("No tienes permiso para confirmar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("La reserva está cancelada");
        }

        if (reservation.getStatus() == ReservationStatus.ATTENDED) {
            throw new BusinessException("La asistencia ya fue confirmada anteriormente");
        }

        reservation.setStatus(ReservationStatus.ATTENDED);
        Reservation saved = reservationRepository.save(reservation);
        gamificationService.onReservationAttended(reservation.getUser());

        String slot = String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + reservation.getDurationHours());

        emailService.sendAttendanceConfirmationToPlayer(
            reservation.getUser().getEmail(),
            reservation.getUser().getName(),
            reservation.getCourt().getName(),
            reservation.getDate().toString(),
            slot
        );

        emailService.sendAttendanceConfirmedToOwner(
            reservation.getCourt().getOwner().getEmail(),
            reservation.getCourt().getOwner().getName(),
            reservation.getUser().getName(),
            reservation.getUser().getEmail(),
            reservation.getCourt().getName(),
            reservation.getDate().toString(),
            slot
        );

        return toResponse(saved);
    }

    /**
     * Cancela todas las reservas PENDING o CONFIRMED del día de hoy para un usuario baneado.
     * Llamado automáticamente desde ChatService cuando se aplica un ban permanente.
     */
    public void cancelTodayReservationsForUser(UUID userId) {
        LocalDate today = LocalDate.now();
        List<Reservation> todayReservations = reservationRepository
            .findActiveReservationsByUserAndDate(
                userId, today,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
            );

        for (Reservation r : todayReservations) {
            r.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(r);
            log.info("Reserva {} cancelada por ban permanente del usuario {}", r.getId(), userId);

            try {
                String slot = String.format("%02d:00 - %02d:00", r.getSlotHour(), r.getSlotHour() + r.getDurationHours());
                emailService.sendReservationCancellation(
                    r.getUser().getEmail(),
                    r.getUser().getName(),
                    r.getCourt().getName(),
                    r.getDate().toString(),
                    slot
                );
            } catch (Exception ex) {
                log.warn("No se pudo enviar email de cancelación por ban: {}", ex.getMessage());
            }
        }

        if (!todayReservations.isEmpty()) {
            log.warn("Se cancelaron {} reserva(s) del día {} para el usuario baneado {}",
                todayReservations.size(), today, userId);
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private ReservationResponse toResponse(Reservation r) {
        String slotLabel = String.format("%02d:00 - %02d:00", r.getSlotHour(), r.getSlotHour() + r.getDurationHours());
        return ReservationResponse.builder()
                .id(r.getId())
                .courtName(r.getCourt().getName())
                .sportType(r.getCourt().getSportType())
                .courtAddress(r.getCourt().getAddress())
                .courtCity(r.getCourt().getCity())
                .courtDistrict(r.getCourt().getDistrict())
                .courtLat(r.getCourt().getLatitude())
                .courtLng(r.getCourt().getLongitude())
                .date(r.getDate())
                .slotHour(r.getSlotHour())
                .durationHours(r.getDurationHours())
                .slotLabel(slotLabel)
                .totalAmount(r.getTotalAmount())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .clientName(r.getUser().getName())
                .clientEmail(r.getUser().getEmail())
                .build();
    }
}