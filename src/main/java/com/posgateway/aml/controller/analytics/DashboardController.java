package com.posgateway.aml.controller.analytics;

import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.MerchantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// @RequiredArgsConstructor removed
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final MerchantRepository merchantRepository;
    private final ComplianceCaseRepository caseRepository;
    private final com.posgateway.aml.repository.UserRepository userRepository;

    public DashboardController(MerchantRepository merchantRepository, ComplianceCaseRepository caseRepository,
            com.posgateway.aml.repository.UserRepository userRepository) {
        this.merchantRepository = merchantRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    private com.posgateway.aml.entity.User getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null)
            return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> stats = new HashMap<>();
        com.posgateway.aml.entity.User user = getCurrentUser();
        Long pspId = (user != null && user.getPsp() != null) ? user.getPsp().getPspId() : null;

        if (pspId != null) {
            stats.put("totalMerchants", merchantRepository.countByPspPspId(pspId));
            stats.put("activeMerchants", merchantRepository.countByPspPspIdAndStatus(pspId, "ACTIVE"));
            stats.put("pendingScreening", merchantRepository.countByPspPspIdAndStatus(pspId, "PENDING_SCREENING"));

            stats.put("openCases", caseRepository.countByPspIdAndStatus(pspId, com.posgateway.aml.model.CaseStatus.NEW)
                    + caseRepository.countByPspIdAndStatus(pspId, com.posgateway.aml.model.CaseStatus.ASSIGNED)
                    + caseRepository.countByPspIdAndStatus(pspId, com.posgateway.aml.model.CaseStatus.IN_PROGRESS));
        } else {
            stats.put("totalMerchants", merchantRepository.count());
            stats.put("activeMerchants", merchantRepository.countByStatus("ACTIVE"));
            stats.put("pendingScreening", merchantRepository.countByStatus("PENDING_SCREENING"));

            stats.put("openCases", caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.NEW)
                    + caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.ASSIGNED)
                    + caseRepository.countByStatus(com.posgateway.aml.model.CaseStatus.IN_PROGRESS));
        }
        stats.put("urgentCases", 0L); // Placeholder

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/cases/priority")
    public ResponseEntity<Map<String, Long>> getCasesByPriority() {
        Map<String, Long> counts = new HashMap<>();
        com.posgateway.aml.entity.User user = getCurrentUser();
        Long pspId = (user != null && user.getPsp() != null) ? user.getPsp().getPspId() : null;

        for (com.posgateway.aml.model.CasePriority p : com.posgateway.aml.model.CasePriority.values()) {
            if (pspId != null) {
                counts.put(p.name(), caseRepository.countByPspIdAndPriority(pspId, p));
            } else {
                counts.put(p.name(), caseRepository.countByPriority(p));
            }
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

    @GetMapping("/sanctions/status")
    public ResponseEntity<Map<String, Object>> getSanctionsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastRun", "Today, 02:00 AM");
        status.put("status", "SUCCESS");
        status.put("merchantsProcessed", 1240);
        status.put("hitsFound", 3);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/fraud-metrics")
    public ResponseEntity<Map<String, Object>> getFraudMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("precision", "98.5%");
        metrics.put("recall", "92.1%");
        metrics.put("f1", "95.2%");
        metrics.put("falsePositives", "0.4%");
        return ResponseEntity.ok(metrics);
    }
}
