package com.posgateway.aml.controller.analytics;

import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MerchantRepository merchantRepository;
    private final ComplianceCaseRepository caseRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalMerchants", merchantRepository.count());
        stats.put("activeMerchants", merchantRepository.countByStatus("ACTIVE"));
        stats.put("pendingScreening", merchantRepository.countByStatus("PENDING_SCREENING"));

        stats.put("openCases", caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.NEW)
                + caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.ASSIGNED)
                + caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.IN_PROGRESS));
        stats.put("urgentCases", 0L); // Placeholder; priority-based counting not implemented here

        return ResponseEntity.ok(stats);
    }

    /**
     * Priority-based case counts
     * GET /api/v1/dashboard/cases/priority
     */
    @GetMapping("/cases/priority")
    public ResponseEntity<Map<String, Long>> getCasesByPriority() {
        Map<String, Long> counts = new HashMap<>();
        for (com.posgateway.aml.model.CasePriority p : com.posgateway.aml.model.CasePriority.values()) {
            counts.put(p.name(), caseRepository.countByPriority(p));
        }
        return ResponseEntity.ok(counts);
    }

    /**
     * Merchant-filtered case stats
     * GET /api/v1/dashboard/cases/merchant/{merchantId}
     */
    @GetMapping("/cases/merchant/{merchantId}")
    public ResponseEntity<Map<String, Long>> getCasesByMerchant(@PathVariable String merchantId) {
        Map<String, Long> stats = new HashMap<>();
        for (com.posgateway.aml.model.CaseStatus s : com.posgateway.aml.model.CaseStatus.values()) {
            stats.put("status_" + s.name(), caseRepository.countByMerchantIdAndStatus(merchantId, s));
        }
        for (com.posgateway.aml.model.CasePriority p : com.posgateway.aml.model.CasePriority.values()) {
            stats.put("priority_" + p.name(), caseRepository.countByMerchantIdAndPriority(merchantId, p));
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/risk-distribution")
    public ResponseEntity<Map<String, Long>> getRiskDistribution() {
        // Mock data or simple aggregation if DB supports it.
        // real imp: merchantRiskScoreRepository.groupByScoreRange()
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("LOW", 150L);
        distribution.put("MEDIUM", 45L);
        distribution.put("HIGH", 12L);
        return ResponseEntity.ok(distribution);
    }
}
