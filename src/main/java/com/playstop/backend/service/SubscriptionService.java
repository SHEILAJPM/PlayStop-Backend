package com.playstop.backend.service;

import com.playstop.backend.dto.response.SubscriptionResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.SubscriptionPlan;
import com.playstop.backend.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionService {

    private final UserRepository userRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.price-pro}")
    private String pricePro;

    @Value("${stripe.price-enterprise}")
    private String priceEnterprise;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public SubscriptionResponse getMySubscription(User owner) {
        return SubscriptionResponse.builder()
                .plan(owner.getPlan())
                .renewsAt(owner.getPlanRenewsAt())
                .hasActiveSubscription(owner.getStripeSubscriptionId() != null)
                .build();
    }

    // Sin transaccion propia: owner ya viene cargado (no hay escritura en
    // este metodo), asi que no hace falta retener una conexion durante la
    // llamada HTTP a Stripe.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createCheckoutSession(User owner, String planParam) {
        SubscriptionPlan plan;
        try {
            plan = SubscriptionPlan.valueOf(planParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan inválido");
        }
        if (plan == SubscriptionPlan.BASICO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Plan Básico no requiere pago");
        }

        String priceId = plan == SubscriptionPlan.PRO ? pricePro : priceEnterprise;
        if (priceId == null || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Este plan aún no está disponible para contratar");
        }

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(frontendUrl + "/propietario/dashboard?plan=activado")
                .setCancelUrl(frontendUrl + "/propietario/dashboard?plan=cancelado")
                .putMetadata("ownerId", owner.getId().toString())
                .putMetadata("plan", plan.name())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                );

        if (owner.getStripeCustomerId() != null) {
            builder.setCustomer(owner.getStripeCustomerId());
        } else {
            builder.setCustomerEmail(owner.getEmail());
        }

        try {
            Session session = Session.create(builder.build());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error creando sesión de suscripción Stripe para owner {}: {}", owner.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error al iniciar la suscripción");
        }
    }

    public void handleCheckoutCompleted(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String ownerId = metadata != null ? metadata.get("ownerId") : null;
        String planStr = metadata != null ? metadata.get("plan") : null;
        if (ownerId == null || planStr == null) return;

        userRepository.findById(java.util.UUID.fromString(ownerId)).ifPresent(owner -> {
            owner.setPlan(SubscriptionPlan.valueOf(planStr));
            owner.setStripeCustomerId(session.getCustomer());
            owner.setStripeSubscriptionId(session.getSubscription());
            userRepository.save(owner);
            log.info("Suscripción {} activada para owner {}", planStr, ownerId);
        });
    }

    public void handleSubscriptionUpdated(Subscription subscription) {
        userRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(owner -> {
            if ("active".equals(subscription.getStatus()) || "trialing".equals(subscription.getStatus())) {
                if (subscription.getItems() != null && !subscription.getItems().getData().isEmpty()) {
                    Long periodEnd = subscription.getItems().getData().get(0).getCurrentPeriodEnd();
                    if (periodEnd != null) {
                        owner.setPlanRenewsAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(periodEnd), ZoneId.systemDefault()));
                    }
                }
            } else if ("past_due".equals(subscription.getStatus()) || "unpaid".equals(subscription.getStatus())
                    || "canceled".equals(subscription.getStatus())) {
                owner.setPlan(SubscriptionPlan.BASICO);
                owner.setStripeSubscriptionId(null);
                owner.setPlanRenewsAt(null);
                log.info("Owner {} degradado a BASICO por estado de suscripción Stripe '{}'", owner.getId(), subscription.getStatus());
            }
            userRepository.save(owner);
        });
    }

    public void handleSubscriptionDeleted(Subscription subscription) {
        userRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(owner -> {
            owner.setPlan(SubscriptionPlan.BASICO);
            owner.setStripeSubscriptionId(null);
            owner.setPlanRenewsAt(null);
            userRepository.save(owner);
            log.info("Suscripción cancelada, owner {} degradado a BASICO", owner.getId());
        });
    }
}
