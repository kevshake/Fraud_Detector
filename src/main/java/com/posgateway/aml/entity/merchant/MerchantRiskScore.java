package com.posgateway.aml.entity.merchant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Merchant risk score entity
 */
@Entity
@Table(name = "merchant_risk_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // Risk Score
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    // Component Scores
    @Column(name = "sanctions_score")
    private Integer sanctionsScore;

    @Column(name = "pep_score")
    private Integer pepScore;

    @Column(name = "country_risk_score")
    private Integer countryRiskScore;

    @Column(name = "industry_risk_score")
    private Integer industryRiskScore;

    @Column(name = "volume_risk_score")
    private Integer volumeRiskScore;

    @Column(name = "business_age_score")
    private Integer businessAgeScore;

    // Decision
    @Column(name = "decision", length = 50)
    private String decision; // APPROVE, REVIEW, REJECT

    @Column(name = "decision_reason", columnDefinition = "text")
    private String decisionReason;

    // Metadata
    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private LocalDateTime calculatedAt = LocalDateTime.now();

    @Column(name = "calculated_by", length = 100)
    private String calculatedBy;

    @Column(name = "rules_version", length = 50)
    private String rulesVersion;
}
