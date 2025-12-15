package com.posgateway.aml.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Scoring Service
 * Calls external ML scoring service (XGBoost model) using REST Assured
 * Handles retries and timeout configuration
 */
@Service
public class ScoringService {

    private static final Logger logger = LoggerFactory.getLogger(ScoringService.class);

    private final RestClientService restClientService;

    @Value("${scoring.service.enabled:true}")
    private boolean scoringEnabled;

    @Value("${scoring.service.url:http://localhost:8000}")
    private String scoringServiceUrl;

    @Value("${scoring.service.retry.max:3}")
    private int maxRetries;

    @Autowired
    public ScoringService(RestClientService restClientService) {
        this.restClientService = restClientService;
    }

    /**
     * Score transaction using ML model
     * Optimized for high throughput with connection pooling
     * 
     * @param txnId Transaction ID
     * @param features Feature map
     * @return Scoring result with score and latency
     */
    public ScoringResult scoreTransaction(Long txnId, Map<String, Object> features) {
        if (!scoringEnabled) {
            logger.debug("Scoring service disabled, returning default score");
            return new ScoringResult(txnId, 0.0, 0L);
        }

        logger.debug("Scoring transaction {} with {} features", txnId, features.size());

        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("txn_id", txnId);
            payload.put("features", features);

            String scoringUrl = scoringServiceUrl + "/score";
            Map<String, Object> response = restClientService.postRequestWithRetry(
                scoringUrl, payload, maxRetries);

            if (response != null) {
                Double score = extractDouble(response.get("score"));
                Long latencyMs = extractLong(response.get("latency_ms"));
                
                // Use our own latency if service doesn't provide it
                if (latencyMs == null || latencyMs == 0) {
                    latencyMs = System.currentTimeMillis() - startTime;
                }
                
                logger.info("Transaction {} scored: score={}, latency={}ms", txnId, score, latencyMs);
                return new ScoringResult(txnId, score, latencyMs);
            }

            logger.warn("Empty response from scoring service for transaction {}", txnId);
            long latencyMs = System.currentTimeMillis() - startTime;
            return new ScoringResult(txnId, 0.0, latencyMs);

        } catch (Exception e) {
            logger.error("Error scoring transaction {}: {}", txnId, e.getMessage());
            long latencyMs = System.currentTimeMillis() - startTime;
            return new ScoringResult(txnId, 0.0, latencyMs);
        }
    }

    private Double extractDouble(Object value) {
        // Early return for null
        if (value == null) {
            return 0.0;
        }
        // Optimize instanceof check - Number is faster than individual type checks
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        // Fallback to string parsing only if not a Number
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Long extractLong(Object value) {
        // Early return for null
        if (value == null) {
            return 0L;
        }
        // Optimize instanceof check - Number is faster than individual type checks
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        // Fallback to string parsing only if not a Number
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Scoring Result
     */
    public static class ScoringResult {
        private final Long txnId;
        private final Double score;
        private final Long latencyMs;

        public ScoringResult(Long txnId, Double score, Long latencyMs) {
            this.txnId = txnId;
            this.score = score;
            this.latencyMs = latencyMs;
        }

        public Long getTxnId() {
            return txnId;
        }

        public Double getScore() {
            return score;
        }

        public Long getLatencyMs() {
            return latencyMs;
        }
    }
}
