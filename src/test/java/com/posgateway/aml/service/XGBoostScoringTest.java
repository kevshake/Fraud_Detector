package com.posgateway.aml.service;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify XGBoost ML scoring service integration.
 * 
 * This test verifies:
 * 1. ScoringService can be instantiated
 * 2. Feature maps can be constructed and sent
 * 3. Scoring response contains expected fields
 * 
 * NOTE: For full ML verification, the XGBoost service must be running on
 * scoring.service.url
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/test-seed-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class XGBoostScoringTest {

    private static final Logger logger = LoggerFactory.getLogger(XGBoostScoringTest.class);

    @Autowired
    private ScoringService scoringService;

    @Value("${scoring.service.enabled:true}")
    private boolean scoringEnabled;

    @Value("${scoring.service.url:http://localhost:8000}")
    private String scoringServiceUrl;

    @Test
    void shouldScoreTransactionWithFeatures() {
        // Arrange: Create test feature map simulating a transaction
        Long testTxnId = 12345L;
        Map<String, Object> features = new HashMap<>();
        features.put("merchant_id", "MERCHANT_001");
        features.put("amount", 1500.00);
        features.put("currency", "KES");
        features.put("channel", "POS");
        features.put("mcc", "5411"); // Grocery stores
        features.put("country_code", "KEN");
        features.put("hour_of_day", 14);
        features.put("day_of_week", 3);
        features.put("is_recurring", false);
        features.put("velocity_1h", 5);
        features.put("velocity_24h", 20);

        logger.info("Testing XGBoost scoring with {} features", features.size());
        logger.info("Scoring service URL: {}", scoringServiceUrl);
        logger.info("Scoring enabled: {}", scoringEnabled);

        // Act: Call the scoring service
        ScoringService.ScoringResult result = scoringService.scoreTransaction(testTxnId, features);

        // Assert: Verify result structure
        assertNotNull(result, "Scoring result should not be null");
        assertEquals(testTxnId, result.getTxnId(), "Transaction ID should match");
        assertNotNull(result.getScore(), "Score should not be null");
        assertNotNull(result.getLatencyMs(), "Latency should be tracked");

        // Log result details
        logger.info("XGBoost Scoring Result:");
        logger.info("  - Transaction ID: {}", result.getTxnId());
        logger.info("  - Score: {}", result.getScore());
        logger.info("  - Latency: {}ms", result.getLatencyMs());
        logger.info("  - Risk Details: {}", result.getRiskDetails());

        // If scoring is enabled, the score should be from XGBoost
        // If disabled, it defaults to 0.0
        if (scoringEnabled) {
            logger.info("XGBoost service should be called at: {}/score", scoringServiceUrl);
            // Score should be between 0.0 and 1.0 for fraud probability
            assertTrue(result.getScore() >= 0.0 && result.getScore() <= 1.0,
                    "Score should be between 0.0 and 1.0, got: " + result.getScore());
        } else {
            logger.warn("Scoring is DISABLED - using fallback score of 0.0");
            assertEquals(0.0, result.getScore(), "Disabled scoring should return 0.0");
        }

        // Risk details should contain scheme simulator results
        Map<String, Object> riskDetails = result.getRiskDetails();
        if (riskDetails != null && !riskDetails.isEmpty()) {
            logger.info("Risk Details from Scheme Simulators:");
            riskDetails.forEach((key, value) -> logger.info("  - {}: {}", key, value));
        }
    }

    @Test
    void shouldHandleMissingXGBoostServiceGracefully() {
        // This test verifies graceful degradation when XGBoost service is unavailable
        Long testTxnId = 99999L;
        Map<String, Object> features = new HashMap<>();
        features.put("merchant_id", "TEST_MERCHANT");
        features.put("amount", 100.00);

        // Act: Should not throw even if XGBoost is down
        assertDoesNotThrow(() -> {
            ScoringService.ScoringResult result = scoringService.scoreTransaction(testTxnId, features);
            assertNotNull(result);
            logger.info("Graceful handling verified - Score: {}", result.getScore());
        }, "Scoring should handle unavailable XGBoost service gracefully");
    }
}
