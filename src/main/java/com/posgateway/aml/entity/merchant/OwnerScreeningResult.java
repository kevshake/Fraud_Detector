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
 * Beneficial Owner screening result entity
 */
@Entity
@Table(name = "owner_screening_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "screening_id")
    private Long screeningId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private BeneficialOwner owner;

    // Screening Details
    @Column(name = "screening_type", nullable = false, length = 50)
    private String screeningType; // ONBOARDING, PERIODIC, UPDATE, MANUAL

    @Column(name = "screening_status", nullable = false, length = 50)
    private String screeningStatus; // CLEAR, MATCH, POTENTIAL_MATCH

    @Column(name = "match_score", precision = 5, scale = 4)
    private BigDecimal matchScore;

    @Column(name = "match_count")
    @Builder.Default
    private Integer matchCount = 0;

    @Column(name = "is_pep")
    @Builder.Default
    private Boolean isPep = false;

    @Column(name = "is_sanctioned")
    @Builder.Default
    private Boolean isSanctioned = false;

    // Results (JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "match_details", columnDefinition = "jsonb")
    private Map<String, Object> matchDetails;

    @Column(name = "screening_provider", length = 100)
    private String screeningProvider; // INTERNAL_AEROSPIKE, SUMSUB

    // Metadata
    @Column(name = "screened_at", nullable = false)
    @Builder.Default
    private LocalDateTime screenedAt = LocalDateTime.now();

    @Column(name = "screened_by", length = 100)
    private String screenedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
