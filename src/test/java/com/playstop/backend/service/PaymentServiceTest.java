package com.playstop.backend.service;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Payment;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.PaymentStatus;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.PaymentRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private SubscriptionService subscriptionService;

    private PaymentService paymentService;

    private UUID userId;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, reservationRepository, reservationService, subscriptionService);
        ReflectionTestUtils.setField(paymentService, "stripeWebhookSecret", "whsec_test");
        ReflectionTestUtils.setField(paymentService, "frontendUrl", "https://playstop.test");

        userId = UUID.randomUUID();
        User user = User.builder().id(userId).name("Jugador").email("jugador@test.com").build();
        Court court = Court.builder().id(UUID.randomUUID()).name("Cancha 1").pricePerHour(BigDecimal.valueOf(50)).build();
        reservation = Reservation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .court(court)
                .slotHour(10)
                .durationHours(1)
                .totalAmount(BigDecimal.valueOf(50))
                .status(ReservationStatus.PENDING)
                .build();
    }

    @Test
    void createCheckoutSession_reservationNotFound_throws404() {
        when(reservationRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession(UUID.randomUUID(), userId));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_notOwner_throws403() {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession(reservation.getId(), UUID.randomUUID()));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_notPending_throws400() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.createCheckoutSession(reservation.getId(), userId));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_success_savesPaymentWithStripeSessionId() throws Exception {
        when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservation(reservation)).thenReturn(Optional.empty());

        Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setUrl("https://checkout.stripe.com/cs_test_123");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(stripeSession);

            String url = paymentService.createCheckoutSession(reservation.getId(), userId);

            assertEquals("https://checkout.stripe.com/cs_test_123", url);
        }

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("cs_test_123", paymentCaptor.getValue().getStripeSessionId());
        assertEquals(PaymentStatus.PENDING, paymentCaptor.getValue().getStatus());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void handleWebhook_invalidSignature_throws400() {
        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent(any(), any(), any()))
                    .thenThrow(mock(SignatureVerificationException.class));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> paymentService.handleWebhook("payload", "bad-signature"));

            assertEquals(400, ex.getStatusCode().value());
        }
        verifyNoInteractions(reservationService, subscriptionService);
    }

    @Test
    void handleWebhook_checkoutCompleted_confirmsReservationAndMarksPaymentPaid() {
        Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setMetadata(Map.of("reservationId", reservation.getId().toString()));

        Payment payment = Payment.builder().reservation(reservation).stripeSessionId("cs_test_123").status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByStripeSessionId("cs_test_123")).thenReturn(Optional.of(payment));
        var notificationData = new ReservationService.ConfirmationNotificationData(
                reservation.getId(), "jugador@test.com", "Jugador", null,
                "Cancha 1", "owner@test.com", "Dueño", "2026-07-20", "10:00 - 11:00", BigDecimal.valueOf(50));
        when(reservationService.confirmReservationPayment(reservation.getId())).thenReturn(notificationData);

        withWebhookEvent("checkout.session.completed", stripeSession,
                () -> paymentService.handleWebhook("payload", "sig"));

        verify(reservationService).confirmReservationPayment(reservation.getId());
        verify(reservationService).sendConfirmationNotifications(notificationData);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    void handleWebhook_checkoutExpired_cancelsReservationAndMarksPaymentFailed() {
        Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setMetadata(Map.of("reservationId", reservation.getId().toString()));

        Payment payment = Payment.builder().reservation(reservation).stripeSessionId("cs_test_123").status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByStripeSessionId("cs_test_123")).thenReturn(Optional.of(payment));

        withWebhookEvent("checkout.session.expired", stripeSession,
                () -> paymentService.handleWebhook("payload", "sig"));

        verify(reservationService).cancelExpiredReservation(reservation.getId());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void handleWebhook_subscriptionMetadata_delegatesToSubscriptionService() {
        Session stripeSession = new Session();
        stripeSession.setId("cs_test_456");
        stripeSession.setMetadata(Map.of("ownerId", UUID.randomUUID().toString(), "plan", "PRO"));

        withWebhookEvent("checkout.session.completed", stripeSession,
                () -> paymentService.handleWebhook("payload", "sig"));

        verify(subscriptionService).handleCheckoutCompleted(stripeSession);
        verifyNoInteractions(reservationService);
    }

    private void withWebhookEvent(String type, com.stripe.model.StripeObject dataObject, Runnable action) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getType()).thenReturn(type);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(dataObject));

        try (MockedStatic<Webhook> webhookStatic = mockStatic(Webhook.class)) {
            webhookStatic.when(() -> Webhook.constructEvent(any(), any(), any())).thenReturn(event);
            action.run();
        }
    }
}
