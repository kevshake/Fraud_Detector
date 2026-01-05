package com.posgateway.aml.controller.reporting;

import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import com.posgateway.aml.repository.AuditLogRepository;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.model.SarStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reporting Summary Controller
 * Provides high-level aggregated data for the dashboard
 */
@RestController
@RequestMapping("/reporting/summary-detailed")
public class ReportingSummaryController {

    private final ComplianceCaseRepository caseRepository;
    private final SuspiciousActivityReportRepository sarRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.posgateway.aml.repository.MerchantRepository merchantRepository;

    public ReportingSummaryController(ComplianceCaseRepository caseRepository,
            SuspiciousActivityReportRepository sarRepository,
            AuditLogRepository auditLogRepository,
            com.posgateway.aml.repository.MerchantRepository merchantRepository) {
        this.caseRepository = caseRepository;
        this.sarRepository = sarRepository;
        this.auditLogRepository = auditLogRepository;
        this.merchantRepository = merchantRepository;
    }

    /**
     * Get summary statistics for dashboard
     * GET /api/v1/reporting/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 1. Cases by Status
        Map<String, Long> casesByStatus = new HashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            casesByStatus.put(status.name(), caseRepository.countByStatus(status));
        }
        summary.put("casesByStatus", casesByStatus);

        // 2. SARs by Status
        Map<String, Long> sarsByStatus = new HashMap<>();
        for (SarStatus status : SarStatus.values()) {
            sarsByStatus.put(status.name(), sarRepository.countByStatus(status));
        }
        summary.put("sarsByStatus", sarsByStatus);

        // 3. Merchant Statistics
        summary.put("totalMerchants", merchantRepository.count());
        summary.put("activeMerchants", merchantRepository.countByStatus("ACTIVE"));
        summary.put("highRiskMerchants",
                merchantRepository.countByRiskLevel("HIGH") + merchantRepository.countByRiskLevel("CRITICAL"));

        // Sum of expected volume as a proxy for total volume
        java.util.List<com.posgateway.aml.entity.merchant.Merchant> merchants = merchantRepository.findAll();
        long totalExpectedVolume = merchants.stream()
                .filter(m -> m.getExpectedMonthlyVolume() != null)
                .mapToLong(com.posgateway.aml.entity.merchant.Merchant::getExpectedMonthlyVolume)
                .sum();
        summary.put("totalVolume", totalExpectedVolume);

        // 4. Audit logs in last 24h
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        summary.put("auditLast24h", auditLogRepository.countByTimestampAfter(yesterday));

        // 4. Cases in last 7 days (trend)
        Map<String, Long> casesLast7d = new TreeMap<>(); // Sorted by date string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            String dayKey = dayStart.format(formatter);
            casesLast7d.put(dayKey, caseRepository.countByCreatedAtBetween(dayStart, dayEnd));
        }
        summary.put("casesLast7d", casesLast7d);

        return ResponseEntity.ok(summary);
    }
}
