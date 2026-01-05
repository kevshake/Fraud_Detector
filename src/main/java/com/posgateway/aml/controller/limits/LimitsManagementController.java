package com.posgateway.aml.controller.limits;

import com.posgateway.aml.entity.limits.*;
import com.posgateway.aml.service.limits.LimitsManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Limits & AML Management
 */
@RestController
@RequestMapping("/limits")
public class LimitsManagementController {

    private final LimitsManagementService limitsService;

    public LimitsManagementController(LimitsManagementService limitsService) {
        this.limitsService = limitsService;
    }

    // Dashboard Stats
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(limitsService.getDashboardStats());
    }

    // Merchant Limits
    @GetMapping("/merchant")
    public ResponseEntity<List<MerchantTransactionLimit>> getAllMerchantLimits() {
        return ResponseEntity.ok(limitsService.getAllMerchantLimits());
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<MerchantTransactionLimit> getMerchantLimit(@PathVariable Long merchantId) {
        MerchantTransactionLimit limit = limitsService.getMerchantLimit(merchantId);
        if (limit == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(limit);
    }

    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<MerchantTransactionLimit> createOrUpdateMerchantLimit(
            @PathVariable Long merchantId,
            @RequestBody MerchantTransactionLimit limit,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.createOrUpdateMerchantLimit(merchantId, limit, userId));
    }

    // Global Limits
    @GetMapping("/global")
    public ResponseEntity<List<GlobalLimit>> getAllGlobalLimits() {
        return ResponseEntity.ok(limitsService.getAllGlobalLimits());
    }

    @PostMapping("/global")
    public ResponseEntity<GlobalLimit> createGlobalLimit(
            @RequestBody GlobalLimit limit,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.createGlobalLimit(limit, userId));
    }

    @PutMapping("/global/{id}")
    public ResponseEntity<GlobalLimit> updateGlobalLimit(
            @PathVariable Long id,
            @RequestBody GlobalLimit limit,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.updateGlobalLimit(id, limit, userId));
    }

    @DeleteMapping("/global/{id}")
    public ResponseEntity<Void> deleteGlobalLimit(@PathVariable Long id) {
        limitsService.deleteGlobalLimit(id);
        return ResponseEntity.ok().build();
    }

    // Risk Thresholds
    @GetMapping("/risk-thresholds")
    public ResponseEntity<List<RiskThreshold>> getAllRiskThresholds() {
        return ResponseEntity.ok(limitsService.getAllRiskThresholds());
    }

    @PostMapping("/risk-thresholds")
    public ResponseEntity<RiskThreshold> createOrUpdateRiskThreshold(
            @RequestBody RiskThreshold threshold,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.createOrUpdateRiskThreshold(threshold, userId));
    }

    // Velocity Rules
    @GetMapping("/velocity-rules")
    public ResponseEntity<List<VelocityRule>> getAllVelocityRules() {
        return ResponseEntity.ok(limitsService.getAllVelocityRules());
    }

    @PostMapping("/velocity-rules")
    public ResponseEntity<VelocityRule> createVelocityRule(
            @RequestBody VelocityRule rule,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.createVelocityRule(rule, userId));
    }

    @PutMapping("/velocity-rules/{id}")
    public ResponseEntity<VelocityRule> updateVelocityRule(
            @PathVariable Long id,
            @RequestBody VelocityRule rule,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.updateVelocityRule(id, rule, userId));
    }

    @DeleteMapping("/velocity-rules/{id}")
    public ResponseEntity<Void> deleteVelocityRule(@PathVariable Long id) {
        limitsService.deleteVelocityRule(id);
        return ResponseEntity.ok().build();
    }

    // Country Compliance
    @GetMapping("/country-compliance")
    public ResponseEntity<List<CountryComplianceRule>> getAllCountryComplianceRules() {
        return ResponseEntity.ok(limitsService.getAllCountryComplianceRules());
    }

    @PostMapping("/country-compliance")
    public ResponseEntity<CountryComplianceRule> createOrUpdateCountryCompliance(
            @RequestBody CountryComplianceRule rule,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(limitsService.createOrUpdateCountryCompliance(rule, userId));
    }
}

