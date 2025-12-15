package com.posgateway.aml.controller.compliance;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import com.posgateway.aml.model.SarStatus;
import com.posgateway.aml.repository.UserRepository;
import com.posgateway.aml.service.SarWorkflowService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SAR Workflow
 */
@RestController
@RequestMapping("/api/v1/compliance/sar/workflow")
@RequiredArgsConstructor
@Slf4j
public class SarWorkflowController {

    private final SarWorkflowService sarWorkflowService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<SuspiciousActivityReport> createSar(@RequestBody CreateSarRequest request) {
        User creator = fetchUser(request.getCreatorUserId());
        SuspiciousActivityReport sar = new SuspiciousActivityReport();
        sar.setSarReference(request.getSarReference());
        sar.setNarrative(request.getNarrative());
        sar.setSuspiciousActivityType(request.getSuspiciousActivityType());
        sar.setJurisdiction(request.getJurisdiction() != null ? request.getJurisdiction() : "UNKNOWN");
        sar.setSarType(request.getSarType());
        SuspiciousActivityReport created = sarWorkflowService.createSarDraft(sar, creator);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/submit")
    public ResponseEntity<SuspiciousActivityReport> submitForReview(@RequestBody IdRequest request) {
        User user = fetchUser(request.getUserId());
        SuspiciousActivityReport updated = sarWorkflowService.submitForReview(request.getSarId(), user);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/approve")
    public ResponseEntity<SuspiciousActivityReport> approve(@RequestBody IdRequest request) {
        User approver = fetchUser(request.getUserId());
        SuspiciousActivityReport updated = sarWorkflowService.approveSar(request.getSarId(), approver);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reject")
    public ResponseEntity<SuspiciousActivityReport> reject(@RequestBody RejectRequest request) {
        User rejector = fetchUser(request.getUserId());
        SuspiciousActivityReport updated = sarWorkflowService.rejectSar(request.getSarId(), rejector, request.getReason());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/file")
    public ResponseEntity<SuspiciousActivityReport> file(@RequestBody FileSarRequest request) {
        User filer = fetchUser(request.getUserId());
        SuspiciousActivityReport updated = sarWorkflowService.markAsFiled(request.getSarId(), request.getFilingReference(), filer);
        return ResponseEntity.ok(updated);
    }

    private User fetchUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Data
    public static class CreateSarRequest {
        private String sarReference;
        private String narrative;
        private String suspiciousActivityType;
        private String jurisdiction;
        private com.posgateway.aml.model.SarType sarType = com.posgateway.aml.model.SarType.INITIAL;
        private Long creatorUserId;
    }

    @Data
    public static class IdRequest {
        private Long sarId;
        private Long userId;
    }

    @Data
    public static class RejectRequest {
        private Long sarId;
        private Long userId;
        private String reason;
    }

    @Data
    public static class FileSarRequest {
        private Long sarId;
        private Long userId;
        private String filingReference;
    }
}

