package com.posgateway.aml.controller;

import com.posgateway.aml.entity.AuditLog;
import com.posgateway.aml.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/entity")
    public ResponseEntity<List<AuditLog>> byEntity(@RequestParam String entityType,
                                                   @RequestParam String entityId) {
        return ResponseEntity.ok(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<AuditLog>> byUser(@PathVariable String username) {
        return ResponseEntity.ok(auditLogRepository.findByUsernameOrderByTimestampDesc(username));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AuditLog>> byRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogRepository.findByTimestampBetween(start, end));
    }
}

