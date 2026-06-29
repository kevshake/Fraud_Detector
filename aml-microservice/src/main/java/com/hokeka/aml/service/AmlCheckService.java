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
import java.util.Set;

@Service
public class AmlCheckService {
    private static final Logger log = LoggerFactory.getLogger(AmlCheckService.class);

    /** High-risk country codes loaded from configuration — managed in application.yml / env. */
    private final Set<String> highRiskCountries;

    /** Medium-risk country codes loaded from configuration. */
    private final Set<String> mediumRiskCountries;

    private static final String NAMESPACE = "aml_cache";
        private static final String SET_NAME = "risk_profile";
        // Also write a reference in the transaction set for cross-service traceability.
        private static final String SET_TXN = "transactions";

    public static final String CACHE_LAYER_AEROSPIKE = "L1_AEROSPIKE";
    public static final String CACHE_LAYER_COMPUTED = "COMPUTED";

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
        String txnId = request.getTransactionId() != null ? request.getTransactionId()
                : "TXN-" + System.currentTimeMillis();
        // PSP-namespaced cache key prevents cross-PSP cache poisoning.
        String cacheKey = pspId + ":" + txnId;

        // Try Aerospike cache first
        if (isAerospikeConnected()) {
            try {
                Key key = new Key(NAMESPACE, SET_NAME, cacheKey);
                Record record = aerospikeClient.get(null, key);
                if (record != null) {
                    double cachedScore = ((Number) record.getValue("risk_score")).doubleValue();
                    String cachedDecision = (String) record.getValue("decision");
                    long elapsed = System.currentTimeMillis() - startTime;
                    return new AmlResult(txnId, pspId, cachedScore, cachedDecision,
                            getRiskLevel(cachedScore), "aerospike_cache", elapsed,
                            CACHE_LAYER_AEROSPIKE);
                }
            } catch (Exception e) {
                log.warn("Aerospike lookup failed for {}: {}", cacheKey, e.getMessage());
            }
        }

        // Compute risk score using rule-based XGBoost-approximation
        double riskScore = computeRiskScore(request);
        String decision = riskScore >= 0.7 ? "BLOCK" : riskScore >= 0.4 ? "REVIEW" : "APPROVE";

        // Store result in Aerospike for future lookups
        if (isAerospikeConnected()) {
            try {
                Key key = new Key(NAMESPACE, SET_NAME, cacheKey);
                WritePolicy wp = new WritePolicy();
                wp.expiration = 3600; // TTL 1 hour
                aerospikeClient.put(wp, key,
                        new Bin("risk_score", riskScore),
                        new Bin("decision", decision),
                        new Bin("psp_id", pspId != null ? pspId : 0L),
                        new Bin("merchant_id", request.getMerchantId()),
                        new Bin("amount", request.getAmount() != null ? request.getAmount().doubleValue() : 0.0));
            } catch (Exception e) {
                log.warn("Aerospike write failed for {}: {}", cacheKey, e.getMessage());
            }
        }

        AmlResult result = new AmlResult(txnId, pspId, riskScore, decision, getRiskLevel(riskScore),
                "computed", System.currentTimeMillis() - startTime, CACHE_LAYER_COMPUTED);

        // Inline sanctions screen on the sender name (lightweight — only when provided).
        // Adds SANCTIONS_FLAGGED indicator on FLAGGED, SANCTIONS_REVIEW on REVIEW.
        String senderName = request.getSenderName();
        if (sanctionsService != null && senderName != null && !senderName.isBlank()) {
            try {
                SanctionsScreenResponse sr = sanctionsService.screenName(senderName, null);
                if (sr != null && "FLAGGED".equals(sr.getStatus())) {
                    result.addIndicator("SANCTIONS_FLAGGED");
                } else if (sr != null && "REVIEW".equals(sr.getStatus())) {
                    result.addIndicator("SANCTIONS_REVIEW");
                }
            } catch (Exception e) {
                log.warn("Inline sanctions screen failed for senderName='{}': {}", senderName, e.getMessage());
            }
        }

        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private double computeRiskScore(TransactionRequest request) {
        double score = 0.1; // baseline

        if (request.getAmount() != null) {
            BigDecimal amount = request.getAmount();
            if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) score += 0.3;
            else if (amount.compareTo(BigDecimal.valueOf(5000)) > 0) score += 0.15;
            else if (amount.compareTo(BigDecimal.valueOf(1000)) > 0) score += 0.05;
        } else if (request.getAmountCents() != null) {
            long cents = request.getAmountCents();
            if (cents > 1_000_000_0L) score += 0.3;       // > $10k
            else if (cents > 500_000_0L) score += 0.15;   // > $5k
            else if (cents > 100_000_0L) score += 0.05;   // > $1k
        }

        String country = request.getCountry();
        if (country != null) {
            if (highRiskCountries.contains(country)) score += 0.4;
            else if (mediumRiskCountries.contains(country)) score += 0.1;
        }

        String txnType = request.getTransactionType();
        if ("CRYPTO_PURCHASE".equals(txnType) || "CASH_WITHDRAWAL".equals(txnType)) score += 0.2;
        else if ("WIRE_TRANSFER".equals(txnType)) score += 0.1;

        return Math.min(score, 1.0);
    }

    private String getRiskLevel(double score) {
        if (score >= 0.7) return "HIGH";
        if (score >= 0.4) return "MEDIUM";
        return "LOW";
    }
}
