package com.posgateway.aml.controller.compliance;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.repository.UserRepository;
import com.posgateway.aml.service.CaseWorkflowService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Compliance Case Workflow
 */
@RestController
@RequestMapping("/api/v1/compliance/cases/workflow")
@RequiredArgsConstructor
@Slf4j
public class CaseWorkflowController {

    private final CaseWorkflowService caseWorkflowService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<ComplianceCase> createCase(@RequestBody CreateCaseRequest request) {
        User creator = fetchUser(request.getCreatorUserId());
        ComplianceCase created = caseWorkflowService.createCase(
                request.getCaseReference(),
                request.getDescription(),
                request.getPriority() != null ? CasePriority.valueOf(request.getPriority()) : CasePriority.MEDIUM,
                creator
        );
        return ResponseEntity.ok(created);
    }

    @PostMapping("/assign")
    public ResponseEntity<ComplianceCase> assignCase(@RequestBody AssignCaseRequest request) {
        User assigner = fetchUser(request.getAssignerUserId());
        ComplianceCase updated = caseWorkflowService.assignCase(
                request.getCaseId(),
                request.getAssigneeUserId(),
                assigner
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/status")
    public ResponseEntity<ComplianceCase> updateStatus(@RequestBody UpdateStatusRequest request) {
        User user = fetchUser(request.getUserId());
        ComplianceCase updated = caseWorkflowService.updateStatus(
                request.getCaseId(),
                CaseStatus.valueOf(request.getStatus()),
                user
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/escalate")
    public ResponseEntity<ComplianceCase> escalate(@RequestBody EscalateCaseRequest request) {
        User user = fetchUser(request.getUserId());
        ComplianceCase updated = caseWorkflowService.escalateCase(
                request.getCaseId(),
                request.getEscalatedToUserId(),
                request.getReason(),
                user
        );
        return ResponseEntity.ok(updated);
    }

    private User fetchUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Data
    public static class CreateCaseRequest {
        private String caseReference;
        private String description;
        private String priority;
        private Long creatorUserId;
    }

    @Data
    public static class AssignCaseRequest {
        private Long caseId;
        private Long assigneeUserId;
        private Long assignerUserId;
    }

    @Data
    public static class UpdateStatusRequest {
        private Long caseId;
        private String status;
        private Long userId;
    }

    @Data
    public static class EscalateCaseRequest {
        private Long caseId;
        private Long escalatedToUserId;
        private String reason;
        private Long userId;
    }
}

