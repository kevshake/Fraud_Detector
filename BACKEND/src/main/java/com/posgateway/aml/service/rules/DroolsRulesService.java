package com.posgateway.aml.service.rules;

import com.posgateway.aml.entity.rules.RuleDefinition;
import com.posgateway.aml.repository.rules.RuleDefinitionRepository;
import com.posgateway.aml.rules.RuleEvaluationResult;
import com.posgateway.aml.rules.TransactionFact;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drools Rules Engine Service for AML Regulatory Compliance.
 * 
 * Provides deterministic rule evaluation for:
 * - Configured regulatory compliance decisions
 * - ML score thresholds
 * - Velocity-based rules
 * - Dynamic Rules from Database (RuleDefinition)
 * 
 * Results cached in Redis for short-lived decision retrieval.
 */
@Service
public class DroolsRulesService {

    private static final Logger logger = LoggerFactory.getLogger(DroolsRulesService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RuleDefinitionRepository ruleRepository;

    private KieContainer kieContainer;
    private boolean droolsEnabled = false;

    @Autowired
    public DroolsRulesService(
            RedisTemplate<String, Object> redisTemplate,
            RuleDefinitionRepository ruleRepository) {
        this.redisTemplate = redisTemplate;
        this.ruleRepository = ruleRepository;
    }

    @PostConstruct
    public void initializeRules() {
        reloadRules();
    }

    public synchronized void reloadRules() {
        logger.info("Initializing/Reloading Drools Rules Engine...");
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kfs = kieServices.newKieFileSystem();
            boolean rulesFound = false;

            // 1. Try to load DRL from classpath (Static Fallback)
            try {
                kfs.write("src/main/resources/rules/aml-rules.drl",
                        kieServices.getResources().newClassPathResource("rules/aml-rules.drl"));
                rulesFound = true;
                logger.debug("Loaded static rules from classpath.");
            } catch (Exception e) {
                logger.debug("No static DRL file found on classpath (this is expected if fully dynamic).");
            }
            
            // 2. Load Dynamic Rules from Database
            List<RuleDefinition> dynamicRules = ruleRepository.findByEnabledTrueOrderByPriorityDesc();
            if (!dynamicRules.isEmpty()) {
                logger.info("loading {} dynamic rules from database.", dynamicRules.size());
                for (RuleDefinition rule : dynamicRules) {
                    if (rule.getDrlContent() != null && !rule.getDrlContent().isBlank()) {
                        String path = "src/main/resources/rules/dynamic/" + rule.getName() + ".drl";
                        kfs.write(path, kieServices.getResources().newByteArrayResource(rule.getDrlContent().getBytes()));
                        rulesFound = true;
                    }
                }
            }

            if (rulesFound) {
                KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

                if (kieBuilder.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)) {
                    logger.error("Drools rule errors: {}", kieBuilder.getResults().getMessages());
                    droolsEnabled = false;
                } else {
                    KieModule kieModule = kieBuilder.getKieModule();
                    this.kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
                    droolsEnabled = true;
                    logger.info("Drools Rules Engine initialized successfully.");
                }
            } else {
                logger.warn("No rules found (static or dynamic). Using programmatic fallback only.");
                droolsEnabled = false;
            }

        } catch (Exception e) {
            logger.error("Unexpected error during Drools engine initialization: {}", e.getMessage(), e);
            droolsEnabled = false;
        }
        if (!droolsEnabled) {
            logger.info("Drools initialized with programmatic rules fallback.");
        }
    }

    /**
     * Evaluate transaction against all AML rules.
     * Uses Drools for rule evaluation and caches results in Redis.
     */
    public RuleEvaluationResult evaluate(Long txnId, Map<String, Object> features, Double mlScore) {
        long startTime = System.currentTimeMillis();

        // Build transaction fact from features
        TransactionFact fact = buildTransactionFact(txnId, features, mlScore);

        // Evaluate rules
        int rulesExecuted = evaluateRules(fact);

        long evaluationTime = System.currentTimeMillis() - startTime;

        RuleEvaluationResult result = new RuleEvaluationResult(
                txnId,
                fact.getDecision(),
                new ArrayList<>(fact.getReasons()),
                new ArrayList<>(fact.getTriggeredRules()),
                fact.isSarRequired(),
                fact.isCtrRequired(),
                rulesExecuted,
                evaluationTime);

        // Cache result in Redis for audit trail (24h TTL, mirrors prior decision-cache behavior)
        try {
            Map<String, Object> entry = new HashMap<>();
            entry.put("decision", fact.getDecision());
            entry.put("finalScore", mlScore);
            entry.put("reasons", fact.getReasons() != null ? String.join("|", fact.getReasons()) : "");
            entry.put("triggeredRules", String.join(",", fact.getTriggeredRules()));
            entry.put("decidedAt", System.currentTimeMillis());
            redisTemplate.opsForValue().set("graph:risk:" + txnId, entry, Duration.ofHours(24));
        } catch (Exception e) {
            logger.warn("Error caching risk decision for txn {}: {}", txnId, e.getMessage());
        }

        logger.info("Rules evaluated for txn {}: decision={}, rules={}, time={}ms",
                txnId, result.getDecision(), result.getTriggeredRules().size(), evaluationTime);

        return result;
    }

    public TransactionFact buildTransactionFactPublic(Long txnId, Map<String, Object> features, Double mlScore) {
        return buildTransactionFact(txnId, features, mlScore);
    }



    private TransactionFact buildTransactionFact(Long txnId, Map<String, Object> features, Double mlScore) {
        TransactionFact fact = new TransactionFact(
                txnId,
                (String) features.getOrDefault("merchant_id", "UNKNOWN"),
                toBigDecimal(features.get("amount")),
                (String) features.getOrDefault("currency", "USD"),
                (String) features.getOrDefault("country_code", "UNK"),
                LocalDateTime.now(),
                (String) features.getOrDefault("channel", "POS"),
                (String) features.get("pan_hash"),
                mlScore,
                toDouble(features.get("pageRank")),
                toLong(features.get("communityId")),
                toDouble(features.get("betweenness")),
                toLong(features.get("connectionCount")),
                toLong(features.get("pan_txn_count_1h")),
                toLong(features.get("pan_txn_count_24h")),
                toBoolean(features.get("cash_transaction")),
                toBoolean(features.get("country_high_risk")),
                toDouble(features.get("pan_txn_amount_sum_24h")),
                toDouble(features.get("merchant_txn_amount_sum_24h")),
                toDouble(features.get("krs_score")),
                toDouble(features.get("cra_score")),
                toDouble(features.get("trs_score")));
        // MCC is enriched into the feature map from the merchant profile; expose it on the
        // fact so DRL/dynamic rules can target specific merchant category codes.
        Object mcc = features.get("mcc");
        if (mcc != null) {
            fact.setMcc(String.valueOf(mcc));
        }
        return fact;
    }

    private int evaluateRules(TransactionFact fact) {
        if (droolsEnabled && kieContainer != null) {
            // Use Drools session
            KieSession session = kieContainer.newKieSession();
            try {
                session.insert(fact);
                return session.fireAllRules();
            } finally {
                session.dispose();
            }
        } else {
            // Use programmatic rules
            return evaluateProgrammaticRules(fact);
        }
    }

    /**
     * Programmatic rules implementation (fallback when DRL not available).
     * These mirror the DRL rules for regulatory compliance.
     */
    private int evaluateProgrammaticRules(TransactionFact fact) {
        int rulesTriggered = 0;

        if (fact.getMlScore() > 0.9) {
            fact.setDecision("BLOCK");
            fact.addTriggeredRule("ML_SCORE_HIGH_RISK");
            fact.addReason(String.format("ML risk score exceeds 0.9 threshold: %.3f", fact.getMlScore()));
            rulesTriggered++;
        } else if (fact.getMlScore() > 0.7 && !"BLOCK".equals(fact.getDecision())) {
            fact.setDecision("HOLD");
            fact.addTriggeredRule("ML_SCORE_MEDIUM_RISK");
            fact.addReason(String.format("ML risk score requires review: %.3f", fact.getMlScore()));
            rulesTriggered++;
        }

        if (fact.getBetweenness() > 0.5 && !"BLOCK".equals(fact.getDecision())) {
            fact.setDecision("HOLD");
            fact.addTriggeredRule("HIGH_BETWEENNESS_HUB");
            fact.addReason(String.format(
                    "Merchant has high betweenness centrality (potential hub): %.3f",
                    fact.getBetweenness()));
            rulesTriggered++;
        }

        if (fact.getPageRank() > 0.8 && fact.isHighValueTransaction()) {
            fact.setSarRequired(true);
            fact.addTriggeredRule("HIGH_INFLUENCE_HIGH_VALUE");
            fact.addReason(String.format(
                    "High-influence merchant (PageRank: %.3f) with high-value transaction",
                    fact.getPageRank()));
            rulesTriggered++;
        }

        if (fact.getPanTxnCount1h() > 10 && !"BLOCK".equals(fact.getDecision())) {
            fact.setDecision("HOLD");
            fact.addTriggeredRule("VELOCITY_BREACH_1H");
            fact.addReason("Card velocity breach: " + fact.getPanTxnCount1h() + " transactions in 1 hour");
            rulesTriggered++;
        }

        if (fact.getMerchantAmountSum24h() > 100000 && !"BLOCK".equals(fact.getDecision())) {
            fact.setDecision("HOLD");
            fact.addTriggeredRule("HIGH_MERCHANT_VOLUME_24H");
            fact.addReason(String.format(
                    "Merchant 24h volume exceeds $100,000: $%.2f",
                    fact.getMerchantAmountSum24h()));
            rulesTriggered++;
        }

        return rulesTriggered;
    }

    // Helper conversion methods
    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        if (value instanceof Number)
            return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Double toDouble(Object value) {
        if (value == null)
            return 0.0;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Long toLong(Object value) {
        if (value == null)
            return 0L;
        if (value instanceof Number)
            return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue;
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
