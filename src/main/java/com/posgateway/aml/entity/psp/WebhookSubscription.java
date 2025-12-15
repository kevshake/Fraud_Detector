package com.posgateway.aml.entity.psp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private String pspId;

    @Column(name = "callback_url", nullable = false, length = 1000)
    private String callbackUrl;

    @Column(name = "event_type", nullable = false)
    private String eventType; // RISK_ALERT, CASE_UPDATE, MERCHANT_STATUS_CHANGE

    @Column(name = "secret_key")
    private String secretKey; // For HMAC signature

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "failure_count")
    @Builder.Default
    private int failureCount = 0;
}
