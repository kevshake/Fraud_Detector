package com.hokeka.aml.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.hokeka.aml.model.AmlResult;
import com.hokeka.aml.model.SanctionsScreenResponse;
import com.hokeka.aml.model.TransactionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AmlCheckService {
    private static final Logger log = LoggerFactory.getLogger(AmlCheckService.class);
    private static final String NAMESPACE = "aml_cache";
    private static final String SET_NAME = "risk_profile";

    public static final String CACHE_LAYER_AEROSPIKE = "L1_AEROSPIKE";
    public static final String CACHE_LAYER_COMPUTED = "COMPUTED";

    private final Set<String> highRiskCountries;
    private final Set<String> mediumRiskCountries;

    @Autowired(required = false)
    private AerospikeClient aerospikeClient;

    @Autowired(required = false)
    private SanctionsService sanctionsService;

    public AmlCheckService(
            @Value("${aml.risk.high-risk-countries:IR,KP,SY,CU,SD}") String highCsv,
            @Value("${aml.risk.medium-risk-countries:NG,RU,CN,VE}") String mediumCsv) {
        this.highRiskCountries = parseCsv(highCsv);
        this.mediumRiskCountries = parseCsv(mediumCsv);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public boolean isAerospikeConnected() {
        return aerospikeClient != null && aerospikeClient.isConnected();
    }

    public AmlResult check(TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        Long pspId = request.getPspId();
        String txnId = request.getTransactionId() != null
                ? request.getTransactionId()
                : "TXN-" + System.currentTimeMillis();
        String cacheKey = (pspId != null ? pspId : 0L) + ":" + txnId;

        AmlResult cached = readCached(cacheKey, txnId, pspId, startTime);
        if (cached != null) return cached;

        double riskScore = computeRiskScore(request);
        List<String> indicators = new ArrayList<>();

        String senderName = request.getSenderName();
        if (senderName != null && !senderName.isBlank()) {
            String sanctionsStatus = screenSender(senderName);
            switch (sanctionsStatus) {
                case "FLAGGED" -> {
                    indicators.add("SANCTIONS_FLAGGED");
                    riskScore = Math.max(riskScore, 0.9);
                }
                case "REVIEW" -> {
                    indicators.add("SANCTIONS_REVIEW");
                    riskScore = Math.max(riskScore, 0.6);
                }
                case "CLEAR" -> {
                    // No risk adjustment.
                }
                default -> {
                    indicators.add("SANCTIONS_UNAVAILABLE");
                    riskScore = Math.max(riskScore, 0.4);
                }
            }
        }

        String decision = decisionFor(riskScore);
        AmlResult result = new AmlResult(txnId, pspId, riskScore, decision, getRiskLevel(riskScore),
                "computed", System.currentTimeMillis() - startTime, CACHE_LAYER_COMPUTED);
        indicators.forEach(result::addIndicator);

        writeCached(cacheKey, request, result);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private AmlResult readCached(String cacheKey, String txnId, Long pspId, long startTime) {
        if (!isAerospikeConnected()) return null;
        try {
            Record record = aerospikeClient.get(null, new Key(NAMESPACE, SET_NAME, cacheKey));
            if (record == null) return null;

            Number scoreBin = (Number) record.getValue("risk_score");
            String decision = (String) record.getValue("decision");
            if (scoreBin == null || decision == null) {
                log.warn("Ignoring incomplete AML cache record for {}", cacheKey);
                return null;
            }

            double score = scoreBin.doubleValue();
            AmlResult result = new AmlResult(txnId, pspId, score, decision, getRiskLevel(score),
                    "aerospike_cache", System.currentTimeMillis() - startTime, CACHE_LAYER_AEROSPIKE);
            Object indicatorBin = record.getValue("indicators");
            if (indicatorBin instanceof List<?> values) {
                values.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .forEach(result::addIndicator);
            }
            return result;
        } catch (Exception e) {
            log.warn("Aerospike lookup failed for {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeCached(String cacheKey, TransactionRequest request, AmlResult result) {
        if (!isAerospikeConnected()) return;
        try {
            WritePolicy policy = new WritePolicy();
            policy.expiration = 3600;
            aerospikeClient.put(policy, new Key(NAMESPACE, SET_NAME, cacheKey),
                    new Bin("risk_score", result.getRiskScore()),
                    new Bin("decision", result.getDecision()),
                    new Bin("indicators", result.getIndicators()),
                    new Bin("psp_id", result.getPspId() != null ? result.getPspId() : 0L),
                    new Bin("merchant_id", request.getMerchantId()),
                    new Bin("amount", request.getAmount() != null ? request.getAmount().doubleValue() : 0.0));
        } catch (Exception e) {
            log.warn("Aerospike write failed for {}: {}", cacheKey, e.getMessage());
        }
    }

    private String screenSender(String senderName) {
        if (sanctionsService == null) return "UNAVAILABLE";
        try {
            SanctionsScreenResponse response = sanctionsService.screenName(senderName, null);
            return response != null && response.getStatus() != null ? response.getStatus() : "UNAVAILABLE";
        } catch (Exception e) {
            log.error("Inline sanctions screen failed for senderName='{}': {}", senderName, e.getMessage());
            return "UNAVAILABLE";
        }
    }

    private double computeRiskScore(TransactionRequest request) {
        double score = 0.1;

        if (request.getAmount() != null) {
            BigDecimal amount = request.getAmount();
            if (amount.compareTo(BigDecimal.valueOf(10_000)) > 0) score += 0.3;
            else if (amount.compareTo(BigDecimal.valueOf(5_000)) > 0) score += 0.15;
            else if (amount.compareTo(BigDecimal.valueOf(1_000)) > 0) score += 0.05;
        } else if (request.getAmountCents() != null) {
            long cents = request.getAmountCents();
            if (cents > 1_000_000L) score += 0.3;
            else if (cents > 500_000L) score += 0.15;
            else if (cents > 100_000L) score += 0.05;
        }

        String country = request.getCountry();
        if (country != null) {
            String normalizedCountry = country.trim().toUpperCase();
            if (highRiskCountries.contains(normalizedCountry)) score += 0.4;
            else if (mediumRiskCountries.contains(normalizedCountry)) score += 0.1;
        }

        String transactionType = request.getTransactionType();
        if ("CRYPTO_PURCHASE".equals(transactionType) || "CASH_WITHDRAWAL".equals(transactionType)) score += 0.2;
        else if ("WIRE_TRANSFER".equals(transactionType)) score += 0.1;

        return Math.min(score, 1.0);
    }

    private String decisionFor(double score) {
        if (score >= 0.7) return "BLOCK";
        if (score >= 0.4) return "REVIEW";
        return "APPROVE";
    }

    private String getRiskLevel(double score) {
        if (score >= 0.7) return "HIGH";
        if (score >= 0.4) return "MEDIUM";
        return "LOW";
    }
}
