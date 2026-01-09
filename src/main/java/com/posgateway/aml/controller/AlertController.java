package com.posgateway.aml.controller;

import com.posgateway.aml.entity.Alert;
import com.posgateway.aml.model.AlertDisposition;
import com.posgateway.aml.repository.AlertRepository;
import com.posgateway.aml.service.alert.AlertDispositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alert Controller
 * Provides endpoints for fetching alerts
 */
@RestController
@RequestMapping("/alerts")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'INVESTIGATOR', 'ANALYST')")
@SuppressWarnings("null") // PathVariable Long parameters and repository Optional returns are safe
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertDispositionService alertDispositionService;

    @Autowired
    public AlertController(AlertRepository alertRepository, AlertDispositionService alertDispositionService) {
        this.alertRepository = alertRepository;
        this.alertDispositionService = alertDispositionService;
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
    @SuppressWarnings("null")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get count of active alerts
     * GET /api/v1/alerts/count/active
     */
    @GetMapping("/count/active")
    public ResponseEntity<Map<String, Long>> getActiveAlertCount() {
        Long count = alertRepository.countByStatus("open");
        Map<String, Long> response = new HashMap<>();
        response.put("count", count != null ? count : 0L);
        return ResponseEntity.ok(response);
    }

    /**
     * Resolve an alert
     * PUT /api/v1/alerts/{id}/resolve
     */
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'INVESTIGATOR')")
    @SuppressWarnings("null")
    public ResponseEntity<Alert> resolveAlert(@PathVariable Long id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setStatus("resolved");
                    Alert saved = alertRepository.save(alert);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an alert
     * DELETE /api/v1/alerts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    @SuppressWarnings("null")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        if (alertRepository.existsById(id)) {
            alertRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get alert disposition statistics
     * GET /api/v1/alerts/disposition-stats
     */
    @GetMapping("/disposition-stats")
    public ResponseEntity<Map<String, Object>> getDispositionStats(
            @RequestParam(required = false) Integer days) {
        int daysBack = days != null ? days : 30;
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        LocalDateTime endDate = LocalDateTime.now();
        
        AlertDispositionService.AlertDispositionStats stats = 
            alertDispositionService.getDispositionStatistics(startDate, endDate);
        Map<AlertDisposition, Long> distribution = 
            alertDispositionService.getDispositionDistribution(startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalAlerts", stats.getTotalAlerts());
        response.put("falsePositives", stats.getFalsePositives());
        response.put("truePositives", stats.getTruePositives());
        response.put("sarFiled", stats.getSarFiled());
        response.put("escalated", stats.getEscalated());
        response.put("falsePositiveRate", stats.getFalsePositiveRate());
        
        // Convert enum keys to strings for JSON
        Map<String, Long> distributionMap = distribution.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().name(),
                Map.Entry::getValue
            ));
        response.put("distribution", distributionMap);
        
        return ResponseEntity.ok(response);
    }
}
