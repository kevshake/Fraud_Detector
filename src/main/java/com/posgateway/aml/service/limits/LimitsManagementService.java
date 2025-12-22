package com.posgateway.aml.service.limits;

import com.posgateway.aml.entity.limits.*;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.repository.MerchantRepository;
import com.posgateway.aml.repository.limits.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing Limits & AML Management
 */
@Service
public class LimitsManagementService {

    private final MerchantTransactionLimitRepository merchantLimitRepository;
    private final GlobalLimitRepository globalLimitRepository;
    private final RiskThresholdRepository riskThresholdRepository;
    private final VelocityRuleRepository velocityRuleRepository;
    private final CountryComplianceRuleRepository countryComplianceRepository;
    private final MerchantRepository merchantRepository;

    public LimitsManagementService(
            MerchantTransactionLimitRepository merchantLimitRepository,
            GlobalLimitRepository globalLimitRepository,
            RiskThresholdRepository riskThresholdRepository,
            VelocityRuleRepository velocityRuleRepository,
            CountryComplianceRuleRepository countryComplianceRepository,
            MerchantRepository merchantRepository) {
        this.merchantLimitRepository = merchantLimitRepository;
        this.globalLimitRepository = globalLimitRepository;
        this.riskThresholdRepository = riskThresholdRepository;
        this.velocityRuleRepository = velocityRuleRepository;
        this.countryComplianceRepository = countryComplianceRepository;
        this.merchantRepository = merchantRepository;
    }

    // Merchant Limits
    @Transactional
    public MerchantTransactionLimit createOrUpdateMerchantLimit(Long merchantId, MerchantTransactionLimit limit, Long userId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        MerchantTransactionLimit existing = merchantLimitRepository.findByMerchant_MerchantId(merchantId)
                .orElse(new MerchantTransactionLimit());

        existing.setMerchant(merchant);
        existing.setDailyLimit(limit.getDailyLimit());
        existing.setWeeklyLimit(limit.getWeeklyLimit());
        existing.setMonthlyLimit(limit.getMonthlyLimit());
        existing.setPerTransactionLimit(limit.getPerTransactionLimit());
        existing.setStatus(limit.getStatus());
        existing.setUpdatedBy(userId);

        return merchantLimitRepository.save(existing);
    }

    public List<MerchantTransactionLimit> getAllMerchantLimits() {
        return merchantLimitRepository.findAll();
    }

    public MerchantTransactionLimit getMerchantLimit(Long merchantId) {
        return merchantLimitRepository.findByMerchant_MerchantId(merchantId)
                .orElse(null);
    }

    // Global Limits
    @Transactional
    public GlobalLimit createGlobalLimit(GlobalLimit limit, Long userId) {
        limit.setCreatedBy(userId);
        return globalLimitRepository.save(limit);
    }

    @Transactional
    public GlobalLimit updateGlobalLimit(Long id, GlobalLimit limit, Long userId) {
        GlobalLimit existing = globalLimitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Global limit not found"));
        existing.setName(limit.getName());
        existing.setDescription(limit.getDescription());
        existing.setLimitType(limit.getLimitType());
        existing.setLimitValue(limit.getLimitValue());
        existing.setPeriod(limit.getPeriod());
        existing.setStatus(limit.getStatus());
        existing.setUpdatedBy(userId);
        return globalLimitRepository.save(existing);
    }

    public List<GlobalLimit> getAllGlobalLimits() {
        return globalLimitRepository.findAll();
    }

    @Transactional
    public void deleteGlobalLimit(Long id) {
        globalLimitRepository.deleteById(id);
    }

    // Risk Thresholds
    @Transactional
    public RiskThreshold createOrUpdateRiskThreshold(RiskThreshold threshold, Long userId) {
        RiskThreshold existing = riskThresholdRepository.findByRiskLevel(threshold.getRiskLevel())
                .orElse(new RiskThreshold());

        existing.setRiskLevel(threshold.getRiskLevel());
        existing.setDescription(threshold.getDescription());
        existing.setDailyLimit(threshold.getDailyLimit());
        existing.setPerTransactionLimit(threshold.getPerTransactionLimit());
        existing.setVelocityLimit(threshold.getVelocityLimit());
        existing.setStatus(threshold.getStatus());
        existing.setUpdatedBy(userId);

        if (existing.getId() == null) {
            existing.setCreatedBy(userId);
        }

        // Update merchant count
        long merchantCount = merchantRepository.findAll().stream()
                .filter(m -> {
                    String merchantRiskLevel = m.getRiskLevel();
                    String thresholdRiskLevel = threshold.getRiskLevel();
                    return merchantRiskLevel != null && merchantRiskLevel.equalsIgnoreCase(thresholdRiskLevel);
                })
                .count();
        existing.setMerchantCount((int) merchantCount);

        return riskThresholdRepository.save(existing);
    }

    public List<RiskThreshold> getAllRiskThresholds() {
        return riskThresholdRepository.findAll();
    }

    // Velocity Rules
    @Transactional
    public VelocityRule createVelocityRule(VelocityRule rule, Long userId) {
        rule.setCreatedBy(userId);
        return velocityRuleRepository.save(rule);
    }

    @Transactional
    public VelocityRule updateVelocityRule(Long id, VelocityRule rule, Long userId) {
        VelocityRule existing = velocityRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Velocity rule not found"));
        existing.setRuleName(rule.getRuleName());
        existing.setDescription(rule.getDescription());
        existing.setMaxTransactions(rule.getMaxTransactions());
        existing.setMaxAmount(rule.getMaxAmount());
        existing.setTimeWindowMinutes(rule.getTimeWindowMinutes());
        existing.setRiskLevel(rule.getRiskLevel());
        existing.setStatus(rule.getStatus());
        existing.setUpdatedBy(userId);
        return velocityRuleRepository.save(existing);
    }

    public List<VelocityRule> getAllVelocityRules() {
        return velocityRuleRepository.findAll();
    }

    @Transactional
    public void deleteVelocityRule(Long id) {
        velocityRuleRepository.deleteById(id);
    }

    // Country Compliance
    @Transactional
    public CountryComplianceRule createOrUpdateCountryCompliance(CountryComplianceRule rule, Long userId) {
        CountryComplianceRule existing = countryComplianceRepository.findByCountryCode(rule.getCountryCode())
                .orElse(new CountryComplianceRule());

        existing.setCountryCode(rule.getCountryCode());
        existing.setCountryName(rule.getCountryName());
        existing.setComplianceRequirements(rule.getComplianceRequirements());
        existing.setTransactionRestrictions(rule.getTransactionRestrictions());
        existing.setRequiredDocumentation(rule.getRequiredDocumentation());
        existing.setStatus(rule.getStatus());
        existing.setUpdatedBy(userId);

        if (existing.getId() == null) {
            existing.setCreatedBy(userId);
        }

        return countryComplianceRepository.save(existing);
    }

    public List<CountryComplianceRule> getAllCountryComplianceRules() {
        return countryComplianceRepository.findAll();
    }

    // Dashboard Statistics
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Active Merchants
        long activeMerchants = merchantRepository.findAll().stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .count();
        stats.put("activeMerchants", activeMerchants);

        // Total Daily Usage
        BigDecimal totalDailyUsage = globalLimitRepository.findAll().stream()
                .filter(g -> "VOLUME".equals(g.getLimitType()) && "DAY".equals(g.getPeriod()))
                .map(GlobalLimit::getCurrentUsage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalDailyUsage", totalDailyUsage);

        // Risk Alerts (merchants with HIGH or CRITICAL risk)
        long riskAlerts = merchantRepository.findAll().stream()
                .filter(m -> {
                    String riskLevel = m.getRiskLevel();
                    return riskLevel != null && 
                           (riskLevel.equalsIgnoreCase("HIGH") || riskLevel.equalsIgnoreCase("CRITICAL"));
                })
                .count();
        stats.put("riskAlerts", riskAlerts);

        // Average Success Rate (mock for now)
        stats.put("avgSuccessRate", 94.2);

        return stats;
    }
}

