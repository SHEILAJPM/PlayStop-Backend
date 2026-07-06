package com.playstop.backend.dto.response;

import com.playstop.backend.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private SubscriptionPlan plan;
    private LocalDateTime renewsAt;
    private boolean hasActiveSubscription;
}
