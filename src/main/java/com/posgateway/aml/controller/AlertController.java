package com.posgateway.aml.controller;

import com.posgateway.aml.entity.Alert;
import com.posgateway.aml.repository.AlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alert Controller
 * Provides endpoints for fetching alerts
 */
@RestController
@RequestMapping("/api/v1/alerts")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'INVESTIGATOR', 'ANALYST')")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Get all alerts
     * GET /api/v1/alerts
     */
    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        List<Alert> alerts;
        if (status != null && !status.isEmpty()) {
            alerts = alertRepository.findByStatus(status);
        } else {
            alerts = alertRepository.findOpenAlerts();
        }
        
        // Limit results
        if (alerts.size() > limit) {
            alerts = alerts.subList(0, limit);
        }
        
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alert by ID
     * GET /api/v1/alerts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return ResponseEntity.of(alertRepository.findById(id));
    }
}
