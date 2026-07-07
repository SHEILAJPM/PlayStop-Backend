package com.playstop.backend.service;

import com.playstop.backend.entity.User;
import com.playstop.backend.enums.SubscriptionPlan;
import com.playstop.backend.repository.UserRepository;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    private SubscriptionService subscriptionService;

    private User owner;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(userRepository);
        ReflectionTestUtils.setField(subscriptionService, "pricePro", "price_pro_123");
        ReflectionTestUtils.setField(subscriptionService, "priceEnterprise", "price_ent_123");
        ReflectionTestUtils.setField(subscriptionService, "frontendUrl", "https://playstop.test");

        owner = User.builder().id(UUID.randomUUID()).name("Dueño").email("owner@test.com")
                .plan(SubscriptionPlan.BASICO).build();
    }

    @Test
    void createCheckoutSession_basicoPlan_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> subscriptionService.createCheckoutSession(owner, "BASICO"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_invalidPlan_throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> subscriptionService.createCheckoutSession(owner, "NOEXISTE"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_priceNotConfigured_throws503() {
        ReflectionTestUtils.setField(subscriptionService, "priceEnterprise", "");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> subscriptionService.createCheckoutSession(owner, "ENTERPRISE"));
        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    void createCheckoutSession_success_returnsCheckoutUrl() {
        Session stripeSession = new Session();
        stripeSession.setUrl("https://checkout.stripe.com/cs_sub_123");

        try (MockedStatic<Session> sessionStatic = mockStatic(Session.class)) {
            sessionStatic.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(stripeSession);

            String url = subscriptionService.createCheckoutSession(owner, "pro");

            assertEquals("https://checkout.stripe.com/cs_sub_123", url);
        }
    }

    @Test
    void handleCheckoutCompleted_activatesPlanAndStoresStripeIds() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        Session stripeSession = new Session();
        stripeSession.setCustomer("cus_123");
        stripeSession.setSubscription("sub_123");
        stripeSession.setMetadata(Map.of("ownerId", owner.getId().toString(), "plan", "PRO"));

        subscriptionService.handleCheckoutCompleted(stripeSession);

        assertEquals(SubscriptionPlan.PRO, owner.getPlan());
        assertEquals("cus_123", owner.getStripeCustomerId());
        assertEquals("sub_123", owner.getStripeSubscriptionId());
    }

    @Test
    void handleSubscriptionUpdated_activeStatus_updatesPlanRenewsAtWithoutChangingPlan() {
        owner.setPlan(SubscriptionPlan.PRO);
        owner.setStripeSubscriptionId("sub_123");
        when(userRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(owner));

        SubscriptionItem item = new SubscriptionItem();
        long periodEnd = Instant.now().plusSeconds(3600).getEpochSecond();
        item.setCurrentPeriodEnd(periodEnd);
        SubscriptionItemCollection items = new SubscriptionItemCollection();
        items.setData(List.of(item));

        Subscription subscription = new Subscription();
        subscription.setId("sub_123");
        subscription.setStatus("active");
        subscription.setItems(items);

        subscriptionService.handleSubscriptionUpdated(subscription);

        assertEquals(SubscriptionPlan.PRO, owner.getPlan());
        assertEquals(
                java.time.LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEnd), java.time.ZoneId.systemDefault()),
                owner.getPlanRenewsAt());
    }

    @Test
    void handleSubscriptionUpdated_pastDue_degradesToBasico() {
        owner.setPlan(SubscriptionPlan.PRO);
        owner.setStripeSubscriptionId("sub_123");
        when(userRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(owner));

        Subscription subscription = new Subscription();
        subscription.setId("sub_123");
        subscription.setStatus("past_due");

        subscriptionService.handleSubscriptionUpdated(subscription);

        assertEquals(SubscriptionPlan.BASICO, owner.getPlan());
        assertNull(owner.getStripeSubscriptionId());
        assertNull(owner.getPlanRenewsAt());
    }

    @Test
    void handleSubscriptionDeleted_degradesToBasicoAndClearsStripeFields() {
        owner.setPlan(SubscriptionPlan.ENTERPRISE);
        owner.setStripeSubscriptionId("sub_123");
        when(userRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(owner));

        Subscription subscription = new Subscription();
        subscription.setId("sub_123");

        subscriptionService.handleSubscriptionDeleted(subscription);

        assertEquals(SubscriptionPlan.BASICO, owner.getPlan());
        assertNull(owner.getStripeSubscriptionId());
        assertNull(owner.getPlanRenewsAt());
    }
}
