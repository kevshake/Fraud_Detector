package com.posgateway.aml.service.monitoring;

import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import com.posgateway.aml.repository.TransactionRepository;
import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Transaction Monitoring Dashboard
 */
@Service
public class TransactionMonitoringService {

    private final TransactionRepository transactionRepository;
    private final SuspiciousActivityReportRepository sarRepository;

    public TransactionMonitoringService(
            TransactionRepository transactionRepository,
            SuspiciousActivityReportRepository sarRepository) {
        this.transactionRepository = transactionRepository;
        this.sarRepository = sarRepository;
    }

    /**
     * Get dashboard statistics for last 24 hours
     */
    public Map<String, Object> getDashboardStats() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<TransactionEntity> recentTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTxnTs() != null && t.getTxnTs().isAfter(last24Hours))
                .collect(Collectors.toList());

        long totalMonitored = recentTransactions.size();
        long flagged = recentTransactions.stream()
                .filter(t -> isFlagged(t))
                .count();
        long highRisk = recentTransactions.stream()
                .filter(t -> isHighRisk(t))
                .count();
        long blocked = recentTransactions.stream()
                .filter(t -> isBlocked(t))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMonitored", totalMonitored);
        stats.put("flagged", flagged);
        stats.put("flagRate", totalMonitored > 0 ? (flagged * 100.0 / totalMonitored) : 0);
        stats.put("highRisk", highRisk);
        stats.put("blocked", blocked);
        return stats;
    }

    /**
     * Get risk score distribution
     */
    public Map<String, Object> getRiskDistribution() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<TransactionEntity> recentTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTxnTs() != null && t.getTxnTs().isAfter(last24Hours))
                .collect(Collectors.toList());

        long low = recentTransactions.stream()
                .filter(t -> getRiskScore(t) >= 0 && getRiskScore(t) <= 25)
                .count();
        long medium = recentTransactions.stream()
                .filter(t -> getRiskScore(t) >= 26 && getRiskScore(t) <= 50)
                .count();
        long high = recentTransactions.stream()
                .filter(t -> getRiskScore(t) >= 51 && getRiskScore(t) <= 75)
                .count();
        long critical = recentTransactions.stream()
                .filter(t -> getRiskScore(t) >= 76 && getRiskScore(t) <= 100)
                .count();

        Map<String, Object> distribution = new HashMap<>();
        distribution.put("low", low);
        distribution.put("medium", medium);
        distribution.put("high", high);
        distribution.put("critical", critical);
        return distribution;
    }

    /**
     * Get top risk indicators
     */
    public List<Map<String, Object>> getTopRiskIndicators() {
        List<Map<String, Object>> indicators = new ArrayList<>();
        
        // Mock data - in real implementation, this would query actual risk indicators
        Map<String, Object> velocity = new HashMap<>();
        velocity.put("name", "Velocity Violations");
        velocity.put("description", "Multiple transactions in short time");
        velocity.put("count", 12);
        indicators.add(velocity);

        Map<String, Object> vpn = new HashMap<>();
        vpn.put("name", "VPN/Proxy Usage");
        vpn.put("description", "IP masking detected");
        vpn.put("count", 8);
        indicators.add(vpn);

        Map<String, Object> geo = new HashMap<>();
        geo.put("name", "Geographic Risk");
        geo.put("description", "High-risk jurisdiction");
        geo.put("count", 5);
        indicators.add(geo);

        Map<String, Object> amount = new HashMap<>();
        amount.put("name", "Amount Threshold");
        amount.put("description", "Exceeds daily limits");
        amount.put("count", 3);
        indicators.add(amount);

        return indicators;
    }

    /**
     * Get recent monitoring activity
     */
    public List<Map<String, Object>> getRecentActivity() {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Get recent transactions and convert to activity log
        List<TransactionEntity> recent = transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getTxnTs().compareTo(a.getTxnTs()))
                .limit(10)
                .collect(Collectors.toList());

        for (TransactionEntity txn : recent) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", getActivityType(txn));
            activity.put("transactionId", "txn-" + txn.getTxnId());
            activity.put("amount", formatAmount(txn));
            activity.put("description", getActivityDescription(txn));
            activity.put("timestamp", txn.getTxnTs());
            activity.put("riskLevel", getRiskLevel(txn));
            activities.add(activity);
        }

        return activities;
    }

    /**
     * Get monitored transactions with filters
     */
    public List<Map<String, Object>> getMonitoredTransactions(String riskLevel, String decision, int limit) {
        return transactionRepository.findAll().stream()
                .sorted((a, b) -> b.getTxnTs().compareTo(a.getTxnTs()))
                .limit(limit)
                .filter(t -> riskLevel == null || riskLevel.equals("All") || getRiskLevel(t).equalsIgnoreCase(riskLevel))
                .filter(t -> decision == null || decision.equals("All") || getDecision(t).equalsIgnoreCase(decision))
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get SARs for monitoring
     */
    public List<Map<String, Object>> getMonitoringSARs() {
        return sarRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toSARDTO)
                .collect(Collectors.toList());
    }

    // Helper methods
    private Map<String, Object> toTransactionDTO(TransactionEntity txn) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", "xn-" + txn.getTxnId());
        dto.put("transactionId", txn.getTxnId());
        dto.put("amount", formatAmount(txn));
        dto.put("currency", txn.getCurrency());
        dto.put("riskScore", getRiskScore(txn));
        dto.put("riskLevel", getRiskLevel(txn));
        dto.put("decision", getDecision(txn));
        dto.put("deviceRisk", getDeviceRisk(txn));
        dto.put("vpnDetected", isVpnDetected(txn));
        dto.put("sanctionsStatus", getSanctionsStatus(txn));
        dto.put("timestamp", txn.getTxnTs());
        dto.put("riskIndicators", getRiskIndicators(txn));
        return dto;
    }

    private Map<String, Object> toSARDTO(SuspiciousActivityReport sar) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", "sar-" + sar.getId());
        dto.put("sarId", sar.getId());
        dto.put("title", sar.getSuspiciousActivityType() != null ? sar.getSuspiciousActivityType().toUpperCase() : "SUSPICIOUS ACTIVITY");
        dto.put("description", sar.getNarrative());
        // Determine priority based on status and activity type
        String priority = "MEDIUM";
        if (sar.getStatus() != null) {
            if (sar.getStatus().name().equals("SUBMITTED") || sar.getStatus().name().equals("ACKNOWLEDGED")) {
                priority = "HIGH";
            }
        }
        dto.put("priority", priority);
        dto.put("status", sar.getStatus() != null ? sar.getStatus().name() : "DRAFT");
        dto.put("createdAt", sar.getCreatedAt());
        dto.put("submitted", sar.getFiledAt() != null);
        dto.put("transactionCount", sar.getSuspiciousTransactions() != null ? sar.getSuspiciousTransactions().size() : 0);
        return dto;
    }

    private String formatAmount(TransactionEntity txn) {
        if (txn.getAmountCents() == null) return "0";
        double amount = txn.getAmountCents() / 100.0;
        return String.format("%s %.2f", txn.getCurrency() != null ? txn.getCurrency() : "USD", amount);
    }

    private int getRiskScore(TransactionEntity txn) {
        // Mock risk score calculation - in real implementation, this would use actual risk assessment
        if (txn.getAmountCents() != null && txn.getAmountCents() > 100000) return 75;
        if (txn.getAmountCents() != null && txn.getAmountCents() > 50000) return 95;
        return 25;
    }

    private String getRiskLevel(TransactionEntity txn) {
        int score = getRiskScore(txn);
        if (score >= 76) return "CRITICAL";
        if (score >= 51) return "HIGH";
        if (score >= 26) return "MEDIUM";
        return "LOW";
    }

    private String getDecision(TransactionEntity txn) {
        String riskLevel = getRiskLevel(txn);
        if ("CRITICAL".equals(riskLevel)) return "DECLINED";
        if ("HIGH".equals(riskLevel)) return "MANUAL_REVIEW";
        return "APPROVED";
    }

    private int getDeviceRisk(TransactionEntity txn) {
        // Mock device risk
        return getRiskScore(txn);
    }

    private boolean isVpnDetected(TransactionEntity txn) {
        // Mock VPN detection
        return getRiskScore(txn) > 50;
    }

    private String getSanctionsStatus(TransactionEntity txn) {
        // Mock sanctions status
        if (getRiskScore(txn) >= 90) return "FLAGGED";
        return "CLEAR";
    }

    private List<String> getRiskIndicators(TransactionEntity txn) {
        List<String> indicators = new ArrayList<>();
        int score = getRiskScore(txn);
        if (score >= 70) indicators.add("AMOUNT: HIGH");
        if (score >= 50) indicators.add("VELOCITY: MEDIUM");
        if (score >= 90) {
            indicators.add("SANCTIONS: CRITICAL");
            indicators.add("GEOGRAPHY: HIGH");
        }
        return indicators;
    }

    private boolean isFlagged(TransactionEntity txn) {
        return getRiskScore(txn) >= 50;
    }

    private boolean isHighRisk(TransactionEntity txn) {
        return getRiskScore(txn) >= 51;
    }

    private boolean isBlocked(TransactionEntity txn) {
        return "DECLINED".equals(getDecision(txn));
    }

    private String getActivityType(TransactionEntity txn) {
        String decision = getDecision(txn);
        if ("DECLINED".equals(decision)) return "blocked";
        if ("MANUAL_REVIEW".equals(decision)) return "review";
        return "approved";
    }

    private String getActivityDescription(TransactionEntity txn) {
        String type = getActivityType(txn);
        String riskLevel = getRiskLevel(txn);
        if ("blocked".equals(type)) {
            return riskLevel + "-risk transaction blocked: " + getSanctionsStatus(txn);
        }
        if ("review".equals(type)) {
            return "Manual review required";
        }
        return "Auto-approved";
    }
}

