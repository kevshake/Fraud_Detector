package com.posgateway.aml.service.risk;

import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.model.Transaction;
import com.posgateway.aml.service.risk.rules.RiskRuleDefinitions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskRulesEngineTest {

    private RiskRulesEngine riskRulesEngine;
    private RiskRuleDefinitions ruleDefinitions;

    @BeforeEach
    void setUp() {
        ruleDefinitions = new RiskRuleDefinitions();
        riskRulesEngine = new RiskRulesEngine(ruleDefinitions);
    }

    @Test
    void testHighValueTransactionRule() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX123");
        tx.setAmount(new BigDecimal("15000.00")); // > 10000
        tx.setCountryCode("US");
        Merchant m = new Merchant();

        List<String> results = riskRulesEngine.evaluateRisk(tx, m);
        assertTrue(results.contains("HIGH_VALUE_TRANSACTION"));
    }

    @Test
    void testHighRiskCountryRule() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX124");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setCountryCode("NK"); // High risk
        Merchant m = new Merchant();

        List<String> results = riskRulesEngine.evaluateRisk(tx, m);
        assertTrue(results.contains("HIGH_RISK_COUNTRY"));
    }

    @Test
    void testUrlMismatchRule() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX125");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setCountryCode("US");
        tx.setTransactionUrl("http://suspicious-site.com");

        Merchant m = new Merchant();
        m.setWebsite("http://legit-shop.com");

        List<String> results = riskRulesEngine.evaluateRisk(tx, m);
        assertTrue(results.contains("URL_MISMATCH_SUSPECTED_LAUNDERING"));
    }
}
