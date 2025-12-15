package com.posgateway.aml.dto.response;

import com.posgateway.aml.model.ScreeningResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for merchant onboarding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOnboardingResponse {

    private Long merchantId;
    private String legalName;
    private String status; // APPROVED, UNDER_REVIEW, REJECTED, PENDING_SCREENING
    private String decision; // APPROVE, REVIEW, REJECT
    private String decisionReason;

    // Screening Results
    private ScreeningResult merchantScreeningResult;
    private List<OwnerScreeningDetail> beneficialOwnerResults;

    // Risk Assessment
    private Integer riskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> riskFactors;

    // Compliance Case (if created)
    private Long complianceCaseId;
    private String caseStatus;
    private String casePriority;

    // Metadata
    private LocalDateTime screenedAt;
    private String screeningProvider; // SUMSUB, AEROSPIKE
    private Double screeningCost;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerScreeningDetail {
        private Long ownerId;
        private String fullName;
        private ScreeningResult screeningResult;
        private Boolean isSanctioned;
        private Boolean isPep;
    }
}
