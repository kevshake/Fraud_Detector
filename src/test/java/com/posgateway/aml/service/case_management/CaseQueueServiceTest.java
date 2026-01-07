package com.posgateway.aml.service.case_management;

import com.posgateway.aml.entity.compliance.CaseQueue;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.repository.CaseQueueRepository;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CaseQueueServiceTest {

    @Test
    void updateQueue_updatesProvidedFields() {
        CaseQueueRepository queueRepo = mock(CaseQueueRepository.class);
        ComplianceCaseRepository caseRepo = mock(ComplianceCaseRepository.class);
        CaseAssignmentService assignmentService = mock(CaseAssignmentService.class);

        CaseQueueService service = new CaseQueueService(queueRepo, caseRepo, assignmentService);

        CaseQueue queue = new CaseQueue();
        queue.setId(1L);
        queue.setQueueName("Q1");
        queue.setEnabled(true);
        queue.setAutoAssign(false);
        queue.setTargetRole("ANALYST");
        queue.setMinPriority(CasePriority.MEDIUM);
        queue.setMaxQueueSize(5);

        when(queueRepo.findById(1L)).thenReturn(Optional.of(queue));
        when(queueRepo.save(any(CaseQueue.class))).thenAnswer(inv -> inv.getArgument(0));

        CaseQueue updated = service.updateQueue(
                1L,
                false,
                true,
                10,
                "COMPLIANCE_OFFICER",
                CasePriority.HIGH
        );

        assertNotNull(updated);
        assertFalse(updated.getEnabled());
        assertTrue(updated.getAutoAssign());
        assertEquals(10, updated.getMaxQueueSize());
        assertEquals("COMPLIANCE_OFFICER", updated.getTargetRole());
        assertEquals(CasePriority.HIGH, updated.getMinPriority());

        verify(queueRepo, times(1)).save(queue);
    }

    @Test
    void processQueue_withNoQueuedCases_doesNotAssign() {
        CaseQueueRepository queueRepo = mock(CaseQueueRepository.class);
        ComplianceCaseRepository caseRepo = mock(ComplianceCaseRepository.class);
        CaseAssignmentService assignmentService = mock(CaseAssignmentService.class);

        CaseQueueService service = new CaseQueueService(queueRepo, caseRepo, assignmentService);

        CaseQueue queue = new CaseQueue();
        queue.setId(1L);
        queue.setQueueName("Q1");
        queue.setEnabled(true);
        queue.setAutoAssign(true);
        queue.setTargetRole("ANALYST");

        when(queueRepo.findById(1L)).thenReturn(Optional.of(queue));
        when(caseRepo.findByQueueAndStatus(queue, CaseStatus.NEW)).thenReturn(List.of());

        service.processQueue(1L);

        verify(caseRepo, times(1)).findByQueueAndStatus(queue, CaseStatus.NEW);
        verifyNoInteractions(assignmentService);
        verify(caseRepo, never()).save(any(ComplianceCase.class));
    }
}


