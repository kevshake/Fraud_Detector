package com.posgateway.aml.service;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.SarStatus;

import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SarWorkflowServiceTest {

    private SuspiciousActivityReportRepository sarRepository;
    private PermissionService permissionService;
    private AuditLogService auditLogService;
    private SarWorkflowService sarWorkflowService;

    @BeforeEach
    void setup() {
        sarRepository = mock(SuspiciousActivityReportRepository.class);
        permissionService = mock(PermissionService.class);
        auditLogService = mock(AuditLogService.class);
        sarWorkflowService = new SarWorkflowService(sarRepository, permissionService, auditLogService);
    }

    @Test
    void approveSar_withPermission_andPendingReview_succeeds() {
        com.posgateway.aml.entity.Role adminRole = new com.posgateway.aml.entity.Role();
        adminRole.setName("ADMIN");

        User approver = new User();
        approver.setId(1L);
        approver.setRole(adminRole);

        SuspiciousActivityReport sar = new SuspiciousActivityReport();
        sar.setId(10L);
        sar.setStatus(SarStatus.PENDING_REVIEW);
        sar.setSarReference("SAR-1");

        when(permissionService.hasPermission(adminRole, Permission.APPROVE_SAR)).thenReturn(true);
        when(sarRepository.findById(10L)).thenReturn(Optional.of(sar));
        when(sarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuspiciousActivityReport updated = sarWorkflowService.approveSar(10L, approver);

        assertEquals(SarStatus.APPROVED, updated.getStatus());
        assertNotNull(updated.getApprovedAt());
        assertEquals(approver, updated.getApprovedBy());
    }

    @Test
    void markAsFiled_requiresApprovedStatus() {
        com.posgateway.aml.entity.Role adminRole = new com.posgateway.aml.entity.Role();
        adminRole.setName("ADMIN");

        User filer = new User();
        filer.setId(2L);
        filer.setRole(adminRole);

        SuspiciousActivityReport sar = new SuspiciousActivityReport();
        sar.setId(11L);
        sar.setStatus(SarStatus.APPROVED);
        sar.setSarReference("SAR-2");

        when(permissionService.hasPermission(adminRole, Permission.FILE_SAR)).thenReturn(true);
        when(sarRepository.findById(11L)).thenReturn(Optional.of(sar));
        when(sarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuspiciousActivityReport updated = sarWorkflowService.markAsFiled(11L, "REF123", filer);

        assertEquals(SarStatus.FILED, updated.getStatus());
        assertEquals("REF123", updated.getFilingReferenceNumber());
        assertEquals(filer, updated.getFiledBy());
        assertNotNull(updated.getFiledAt());
    }
}
