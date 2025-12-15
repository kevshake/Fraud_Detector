package com.posgateway.aml.entity.merchant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * External AML provider response entity (Sumsub, etc.)
 * Stores raw API responses for audit trail
 */
@Entity
@Table(name = "external_aml_responses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAmlResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long responseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private BeneficialOwner owner;

    // Provider Details
    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName; // SUMSUB, COMPLYADVANTAGE

    @Column(name = "screening_type", nullable = false, length = 50)
    private String screeningType; // MERCHANT, BENEFICIAL_OWNER

    // Request/Response (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;

    @Column(name = "response_status", length = 50)
    private String responseStatus; // SUCCESS, ERROR, TIMEOUT

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    // Results Summary
    @Column(name = "sanctions_match")
    @Builder.Default
    private Boolean sanctionsMatch = false;

    @Column(name = "pep_match")
    @Builder.Default
    private Boolean pepMatch = false;

    @Column(name = "adverse_media_match")
    @Builder.Default
    private Boolean adverseMediaMatch = false;

    @Column(name = "overall_risk_level", length = 50)
    private String overallRiskLevel;

    // Billing
    @Column(name = "cost_amount", precision = 10, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "cost_currency", length = 3)
    @Builder.Default
    private String costCurrency = "USD";

    // Metadata
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "screened_by", length = 100)
    private String screenedBy;
}
