package com.posgateway.aml.entity.psp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Billing Rate Entity
 * Configurable pricing for different services
 */
@Entity
@Table(name = "billing_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id")
    private Long rateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id")
    private Psp psp; // NULL = default rate for all PSPs

    // Service Pricing
    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "pricing_model", nullable = false, length = 50)
    private String pricingModel; // PER_REQUEST, TIERED, SUBSCRIPTION, HYBRID

    // Per-Request Pricing
    @Column(name = "base_rate", precision = 10, scale = 4)
    private BigDecimal baseRate;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    // Tiered Pricing (JSON configuration)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tier_config", columnDefinition = "jsonb")
    private Map<String, Object> tierConfig;

    // Subscription Pricing
    @Column(name = "monthly_fee", precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "included_requests")
    private Integer includedRequests;

    @Column(name = "overage_rate", precision = 10, scale = 4)
    private BigDecimal overageRate;

    // Validity Period
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Metadata
    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Helper Methods
    public boolean isEffective(LocalDate date) {
        if (!isActive)
            return false;
        if (date.isBefore(effectiveFrom))
            return false;
        if (effectiveTo != null && date.isAfter(effectiveTo))
            return false;
        return true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
