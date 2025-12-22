package com.posgateway.aml.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings Controller
 * Provides endpoints for system settings and configuration
 */
@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("hasAnyRole('ADMIN')")
public class SettingsController {

    /**
     * Get system settings
     * GET /api/v1/settings
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("theme", "light");
        settings.put("notifications", true);
        settings.put("autoRefresh", true);
        settings.put("refreshInterval", 30); // seconds
        settings.put("timezone", "UTC");
        settings.put("dateFormat", "YYYY-MM-DD");
        settings.put("itemsPerPage", 50);
        return ResponseEntity.ok(settings);
    }

    /**
     * Update system settings
     * PUT /api/v1/settings
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        // In a real implementation, this would save to database
        // For now, just return the settings
        return ResponseEntity.ok(settings);
    }

    /**
     * Get application configuration
     * GET /api/v1/settings/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("appName", "AML Fraud Detector");
        config.put("version", "1.0.0");
        config.put("environment", System.getProperty("spring.profiles.active", "development"));
        config.put("database", "PostgreSQL");
        config.put("cache", "Aerospike");
        return ResponseEntity.ok(config);
    }
}
