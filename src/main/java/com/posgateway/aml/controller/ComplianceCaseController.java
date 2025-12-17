package com.posgateway.aml.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for compliance case management
 * 
 * Security: All endpoints require authentication + specific role/permission
 */
// @Slf4j removed
// @RequiredArgsConstructor removed
@RestController
@RequestMapping("/api/v1/compliance/cases")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'INVESTIGATOR', 'CASE_MANAGER', 'AUDITOR')")
public class ComplianceCaseController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceCaseController.class);

    private final ComplianceCaseRepository complianceCaseRepository;

    public ComplianceCaseController(ComplianceCaseRepository complianceCaseRepository) {
        this.complianceCaseRepository = complianceCaseRepository;
    }

    /**
     * Get all compliance cases
     * GET /api/v1/compliance/cases
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_CASES')")
    public ResponseEntity<List<ComplianceCase>> getAllCases(
            @RequestParam(required = false) String status) {

        log.info("Get all compliance cases (status filter: {})", status);

        List<ComplianceCase> cases;
        if (status != null && !status.isEmpty()) {
            try {
                CaseStatus cs = CaseStatus.valueOf(status);
                cases = complianceCaseRepository.findByStatus(cs);
            } catch (IllegalArgumentException e) {
                cases = List.of();
            }
        } else {
            cases = complianceCaseRepository.findAll();
        }

        return ResponseEntity.ok(cases);
    }

    /**
     * Get case by ID
     * GET /api/v1/compliance/cases/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_CASES')")
    public ResponseEntity<ComplianceCase> getCaseById(@PathVariable Long id) {
        return ResponseEntity.of(complianceCaseRepository.findById(id));
    }

    /**
     * Get statistics
     * GET /api/v1/compliance/cases/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<CaseStats> getStats() {
        long openCases = complianceCaseRepository.countByStatus(CaseStatus.NEW)
                + complianceCaseRepository.countByStatus(CaseStatus.ASSIGNED)
                + complianceCaseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        long inProgressCases = complianceCaseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        long totalCases = complianceCaseRepository.count();

        CaseStats stats = new CaseStats(openCases, inProgressCases, totalCases);

        return ResponseEntity.ok(stats);
    }

    public static class CaseStats {
        private long openCases;
        private long inProgressCases;
        private long totalCases;

        public CaseStats(long openCases, long inProgressCases, long totalCases) {
            this.openCases = openCases;
            this.inProgressCases = inProgressCases;
            this.totalCases = totalCases;
        }

        public long getOpenCases() {
            return openCases;
        }

        public long getInProgressCases() {
            return inProgressCases;
        }

        public long getTotalCases() {
            return totalCases;
        }
    }
}
