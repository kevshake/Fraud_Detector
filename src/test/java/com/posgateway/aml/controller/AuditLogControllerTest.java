package com.posgateway.aml.controller;

import com.posgateway.aml.entity.AuditLog;
import com.posgateway.aml.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuditLogControllerTest {

    @Test
    void getAllAuditLogs_clampsLimit_andSortsByTimestampDesc() {
        AuditLogRepository repo = mock(AuditLogRepository.class);

        AuditLog log = AuditLog.builder()
                .userId("1")
                .username("alice")
                .userRole("ADMIN")
                .actionType("UPDATE")
                .entityType("CASE")
                .entityId("CASE-1")
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        AuditLogController controller = new AuditLogController(repo);

        ResponseEntity<List<AuditLog>> resp = controller.getAllAuditLogs(
                5000,
                "ali",
                "UPDATE",
                "CASE",
                "CASE",
                true,
                null,
                null,
                null,
                null
        );

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo, times(1)).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(1000, pageable.getPageSize()); // clamped

        Sort.Order tsOrder = pageable.getSort().getOrderFor("timestamp");
        assertNotNull(tsOrder);
        assertEquals(Sort.Direction.DESC, tsOrder.getDirection());
    }
}


