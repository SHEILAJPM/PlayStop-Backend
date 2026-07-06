package com.playstop.backend.service;

import com.playstop.backend.entity.Payment;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.enums.PaymentStatus;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.PaymentRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createCheckoutSession(UUID reservationId, UUID currentUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre esta reserva");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta reserva ya no está pendiente de pago");
        }

        BigDecimal amount = reservation.getTotalAmount();
        long unitAmountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        String slot = String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + 1);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/reservas/" + reservation.getId() + "/confirmacion?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/reservar/" + reservation.getCourt().getId() + "?pago=cancelado")
                .setExpiresAt(Instant.now().plus(40, ChronoUnit.MINUTES).getEpochSecond())
                .putMetadata("reservationId", reservation.getId().toString())
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("pen")
                                .setUnitAmount(unitAmountCents)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(reservation.getCourt().getName() + " — " + reservation.getDate() + " " + slot)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();

        try {
            Session session = Session.create(params);

            Payment payment = paymentRepository.findByReservation(reservation)
                    .orElseGet(() -> Payment.builder().reservation(reservation).build());
            payment.setAmount(amount);
            payment.setStripeSessionId(session.getId());
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error creando sesión de pago Stripe para reserva {}: {}", reservationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al iniciar el pago");
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Firma de webhook de Stripe inválida: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma inválida");
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) return;
        Object stripeObject = deserializer.getObject().get();
        if (!(stripeObject instanceof Session session)) return;

        String reservationIdStr = session.getMetadata() != null ? session.getMetadata().get("reservationId") : null;
        if (reservationIdStr == null) {
            log.warn("Evento de Stripe {} sin reservationId en metadata", event.getType());
            return;
        }
        UUID reservationId = UUID.fromString(reservationIdStr);

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(java.time.LocalDateTime.now());
                    paymentRepository.save(payment);
                });
                reservationService.confirmReservationPayment(reservationId);
                log.info("Pago confirmado vía Stripe para reserva {}", reservationId);
            }
            case "checkout.session.expired" -> {
                paymentRepository.findByStripeSessionId(session.getId()).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                });
                reservationService.cancelExpiredReservation(reservationId);
                log.info("Sesión de pago expirada para reserva {}, reserva cancelada", reservationId);
            }
            default -> { /* evento no manejado */ }
        }
    }
}
