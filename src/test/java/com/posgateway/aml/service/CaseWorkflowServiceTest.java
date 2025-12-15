package com.posgateway.aml.service;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.UserRole;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CaseWorkflowServiceTest {

    private ComplianceCaseRepository caseRepository;
    private UserRepository userRepository;
    private PermissionService permissionService;
    private CaseWorkflowService caseWorkflowService;

    @BeforeEach
    void setup() {
        caseRepository = mock(ComplianceCaseRepository.class);
        userRepository = mock(UserRepository.class);
        permissionService = mock(PermissionService.class);
        caseWorkflowService = new CaseWorkflowService(caseRepository, userRepository, permissionService);
    }

    @Test
    void createCase_withPermission_succeeds() {
        User creator = new User();
        creator.setId(1L);
        creator.setRole(UserRole.ADMIN);

        when(permissionService.hasPermission(UserRole.ADMIN, Permission.CREATE_CASES)).thenReturn(true);
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ComplianceCase created = caseWorkflowService.createCase("CASE-1", "desc", CasePriority.HIGH, creator);

        assertNotNull(created);
        assertEquals(CaseStatus.NEW, created.getStatus());
        verify(caseRepository).save(any());
    }

    @Test
    void assignCase_withPermission_updatesAssignee() {
        User assigner = new User();
        assigner.setId(10L);
        assigner.setRole(UserRole.ADMIN);
        User assignee = new User();
        assignee.setId(20L);

        ComplianceCase cc = ComplianceCase.builder().id(100L).status(CaseStatus.NEW).build();

        when(permissionService.hasPermission(UserRole.ADMIN, Permission.ASSIGN_CASES)).thenReturn(true);
        when(caseRepository.findById(100L)).thenReturn(Optional.of(cc));
        when(userRepository.findById(20L)).thenReturn(Optional.of(assignee));
        when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ComplianceCase updated = caseWorkflowService.assignCase(100L, 20L, assigner);

        assertEquals(assignee, updated.getAssignedTo());
        assertEquals(CaseStatus.ASSIGNED, updated.getStatus());
    }
}

