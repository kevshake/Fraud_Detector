package com.posgateway.aml.controller.case_management;

import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.service.case_management.*;
import com.posgateway.aml.service.analytics.ComplianceDashboardService;
import com.posgateway.aml.service.analytics.OperationalMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Case Management Controller
 * Provides endpoints for case management operations
 */
@RestController
@RequestMapping("/cases")
public class CaseManagementController {

    private final CaseActivityService caseActivityService;
    private final CaseAssignmentService caseAssignmentService;
    private final CaseSlaService caseSlaService;
    private final CaseEscalationService caseEscalationService;
    private final CaseQueueService caseQueueService;
    private final CaseTimelineService caseTimelineService;
    private final ComplianceDashboardService dashboardService;
    private final OperationalMetricsService metricsService;
    private final ComplianceCaseRepository caseRepository;

    @Autowired
    public CaseManagementController(CaseActivityService caseActivityService,
                                   CaseAssignmentService caseAssignmentService,
                                   CaseSlaService caseSlaService,
                                   CaseEscalationService caseEscalationService,
                                   CaseQueueService caseQueueService,
                                   CaseTimelineService caseTimelineService,
                                   ComplianceDashboardService dashboardService,
                                   OperationalMetricsService metricsService,
                                   ComplianceCaseRepository caseRepository) {
        this.caseActivityService = caseActivityService;
        this.caseAssignmentService = caseAssignmentService;
        this.caseSlaService = caseSlaService;
        this.caseEscalationService = caseEscalationService;
        this.caseQueueService = caseQueueService;
        this.caseTimelineService = caseTimelineService;
        this.dashboardService = dashboardService;
        this.metricsService = metricsService;
        this.caseRepository = caseRepository;
    }

    /**
     * Get case timeline
     */
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<CaseTimelineService.CaseTimelineDTO> getCaseTimeline(@PathVariable Long caseId) {
        return ResponseEntity.ok(caseTimelineService.buildTimeline(caseId));
    }

    /**
     * Get case activity feed
     */
    @GetMapping("/{caseId}/activities")
    public ResponseEntity<?> getCaseActivities(@PathVariable Long caseId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(caseActivityService.getActivityFeed(
                caseId,
                org.springframework.data.domain.PageRequest.of(page, size)
        ));
    }

    /**
     * Assign case automatically
     */
    @PostMapping("/{caseId}/assign/auto")
    public ResponseEntity<Map<String, Object>> autoAssignCase(@PathVariable Long caseId,
                                                             @RequestParam String role) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        
        com.posgateway.aml.model.UserRole userRole = com.posgateway.aml.model.UserRole.valueOf(role);
        var assignedUser = caseAssignmentService.assignCaseByWorkload(complianceCase, userRole);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "assignedTo", assignedUser.getUsername(),
                "message", "Case assigned successfully"
        ));
    }

    /**
     * Escalate case
     */
    @PostMapping("/{caseId}/escalate")
    public ResponseEntity<Map<String, Object>> escalateCase(@PathVariable Long caseId,
                                                            @RequestBody EscalationRequest request,
                                                            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername()); // Assuming username is user ID
        caseEscalationService.escalateCase(
                caseId,
                request.getReason(),
                userId
        );
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Case escalated successfully"
        ));
    }

    /**
     * Get SLA status
     */
    @GetMapping("/{caseId}/sla")
    public ResponseEntity<Map<String, Object>> getSlaStatus(@PathVariable Long caseId) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found"));
        
        CaseSlaService.CaseSlaStatus slaStatus = caseSlaService.checkSlaStatus(complianceCase);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("slaStatus", slaStatus.name());
        response.put("slaDeadline", complianceCase.getSlaDeadline());
        response.put("daysOpen", complianceCase.getDaysOpen() != null ? complianceCase.getDaysOpen() : 0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get compliance dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ComplianceDashboardService.ComplianceDashboardDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardMetrics());
    }

    /**
     * Get operational metrics
     */
    @GetMapping("/metrics/operational")
    public ResponseEntity<Map<String, Object>> getOperationalMetrics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        
        return ResponseEntity.ok(Map.of(
                "averageInvestigationTime", metricsService.calculateAverageInvestigationTime(startDate, endDate),
                "sarFilingMetrics", metricsService.getSarFilingMetrics(startDate, endDate)
        ));
    }

    /**
     * Get all case queues
     */
    @GetMapping("/queues")
    public ResponseEntity<?> getAllQueues() {
        return ResponseEntity.ok(caseQueueService.getAllQueues());
    }

    // DTOs
    public static class EscalationRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}

