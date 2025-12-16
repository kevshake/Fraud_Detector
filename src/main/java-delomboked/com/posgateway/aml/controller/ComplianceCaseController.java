package com.posgateway.aml.controller;

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
@RestController
@RequestMapping("/api/v1/compliance/cases")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'INVESTIGATOR', 'CASE_MANAGER', 'AUDITOR')")
public class ComplianceCaseController {

    private final ComplianceCaseRepository complianceCaseRepository;

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

        CaseStats stats = CaseStats.builder()
                .openCases(openCases)
                .inProgressCases(inProgressCases)
                .totalCases(totalCases)
                .build();

        return ResponseEntity.ok(stats);
    }

    @lombok.Data
    @lombok.Builder
    private static class CaseStats {
        private long openCases;
        private long inProgressCases;
        private long totalCases;
    }
}
