# Scoring Process Documentation

## Overview

The AML Fraud Detector uses a multi-layered scoring architecture that combines machine learning models, rule-based systems, and anomaly detection to assess transaction risk. This document details how scores are calculated and how the engine arrives at final risk assessments.

## Scoring Architecture

The scoring process follows a pipeline architecture with multiple stages:

```
Transaction → Feature Extraction → ML Scoring → Rule Evaluation → Anomaly Detection → Score Aggregation → Decision
```

## 1. Feature Extraction

**Service:** `FeatureExtractionService`

Before scoring, the system extracts comprehensive features from transactions. These features are categorized as follows:

### 1.1 Transaction-Level Features

- **Amount Features:**
  - `amount`: Transaction amount in base currency
  - `log_amount`: Natural logarithm of amount (for normalization)
  
- **Currency & Identification:**
  - `currency`: Transaction currency code (default: USD)
  - `merchant_id`: Merchant identifier
  - `terminal_id`: Terminal identifier
  - `card_bin_hash`: First 6 digits of PAN hash
  
- **Temporal Features:**
  - `txn_hour_of_day`: Hour of day (0-23)
  - `txn_day_of_week`: Day of week (1-7)

### 1.2 Behavioral Features

**Merchant Velocity:**
- `merchant_txn_count_1h`: Number of transactions for merchant in last 1 hour
- `merchant_txn_amount_sum_24h`: Sum of transaction amounts for merchant in last 24 hours

**PAN (Card) Velocity:**
- `pan_txn_count_1h`: Number of transactions for PAN in last 1 hour
- `pan_txn_amount_sum_7d`: Sum of transaction amounts for PAN in last 7 days
- `distinct_terminals_last_30d_for_pan`: Number of distinct terminals used by PAN in last 30 days
- `avg_amount_by_pan_30d`: Average transaction amount for PAN over 30 days
- `time_since_last_txn_for_pan_minutes`: Minutes since last transaction for this PAN
- `zscore_amount_vs_pan_history`: Z-score comparing current amount to PAN's historical average

### 1.3 EMV Features

- `is_chip_present`: Boolean indicating chip card present
- `is_contactless`: Boolean indicating contactless transaction
- `cvm_method`: Cardholder Verification Method (from EMV tag 9F34)
- `aip_flags`: Application Interchange Profile flags (EMV tag 82)
- `aid`: Application Identifier (EMV tag 4F)
- `approval_code_present`: Boolean indicating approval code present

### 1.4 AML Features

- `cumulative_debits_30d`: Cumulative debit amount for PAN over 30 days
- `num_high_value_txn_7d`: Number of high-value transactions in last 7 days

### 1.5 Graph Features (Neo4j GDS)

- `pageRank`: PageRank centrality score (0-1)
- `communityId`: Community/cluster identifier
- `betweenness`: Betweenness centrality score (0-1)
- `connectionCount`: Number of connections in graph
- `triangle_count`: Number of triangles in graph
- `clustering_coefficient`: Local clustering coefficient (0-1)

### 1.6 Scheme Simulator Features

**VFMP (Visa Fraud Monitoring Program) Simulator:**
- `vfmp_stage`: VFMP stage (NORMAL, MONITORING, WARNING, CRITICAL)
- `merchant_fraud_rate`: Calculated fraud rate for merchant

**HECM (High Error Code Merchant) Simulator:**
- `hecm_stage`: HECM stage (NORMAL, MONITORING, WARNING, CRITICAL)
- `merchant_cb_ratio`: Chargeback ratio for merchant

## 2. Machine Learning Scoring (XGBoost)

**Service:** `ScoringService`

### 2.1 Process Flow

1. **Cache Check:** First checks Aerospike cache for previously computed scores
2. **Scheme Simulator Execution:** Runs VFMP and HECM simulators to enrich features
3. **ML Model Invocation:** Sends features to external XGBoost scoring service via REST API
4. **Score Extraction:** Extracts ML score from response (0.0 to 1.0)

### 2.2 ML Score Range

- **Score Range:** 0.0 (low risk) to 1.0 (high risk)
- **Interpretation:**
  - 0.0 - 0.3: Low risk
  - 0.3 - 0.7: Medium risk
  - 0.7 - 0.9: High risk
  - 0.9 - 1.0: Very high risk

### 2.3 XGBoost Model

The XGBoost model is trained on historical transaction data and learns patterns from:
- Transaction amounts and frequencies
- Merchant behavior patterns
- Card usage patterns
- Temporal patterns
- Graph-based relationships
- Scheme simulator outputs

**Note:** The actual XGBoost model is hosted as a separate microservice and called via REST API at `${scoring.service.url}/score`.

## 3. Rule-Based Scoring (Drools Rules Engine)

**Service:** `DroolsRulesService`

The Drools rules engine applies deterministic regulatory and business rules that can override or modify ML scores.

### 3.1 Rule Evaluation Process

1. **Fact Building:** Transaction features are converted to `TransactionFact` objects
2. **Rule Execution:** Drools engine evaluates all applicable rules
3. **Decision Assignment:** Rules assign decisions (BLOCK, HOLD, ALLOW) and flags (SAR_REQUIRED, CTR_REQUIRED)

### 3.2 Key Rules

#### Rule 1: CTR Threshold
- **Condition:** Transaction amount ≥ $10,000 USD
- **Action:** Flag `CTR_REQUIRED = true`
- **Rule ID:** `CTR_THRESHOLD_10K`

#### Rule 2: SAR Structuring Detection
- **Condition:** Amount between $9,000-$10,000 AND ≥ 3 transactions in 1 hour
- **Action:** `SAR_REQUIRED = true`, `DECISION = HOLD`
- **Rule ID:** `SAR_STRUCTURING_DETECTION`

#### Rule 3: OFAC High-Risk Country Block
- **Condition:** Transaction involves high-risk/sanctioned country
- **Action:** `DECISION = BLOCK`, `SAR_REQUIRED = true`
- **Rule ID:** `OFAC_HIGH_RISK_COUNTRY`

#### Rule 4: High ML Score Block
- **Condition:** ML score > 0.9
- **Action:** `DECISION = BLOCK`
- **Rule ID:** `ML_SCORE_HIGH_RISK`

#### Rule 5: Medium ML Score Hold
- **Condition:** ML score > 0.7 AND ML score ≤ 0.9
- **Action:** `DECISION = HOLD`
- **Rule ID:** `ML_SCORE_MEDIUM_RISK`

#### Rule 6: High Betweenness Centrality
- **Condition:** Betweenness centrality > 0.5
- **Action:** `DECISION = HOLD`
- **Rule ID:** `HIGH_BETWEENNESS_HUB`
- **Rationale:** High betweenness indicates potential money mule hub

#### Rule 7: Velocity Breach
- **Condition:** PAN transaction count in 1 hour > 10
- **Action:** `DECISION = HOLD`
- **Rule ID:** `VELOCITY_BREACH_1H`

#### Rule 8: High Influence + High Value
- **Condition:** PageRank > 0.8 AND high-value transaction
- **Action:** `SAR_REQUIRED = true`
- **Rule ID:** `HIGH_INFLUENCE_HIGH_VALUE`

### 3.3 Score Override Logic

Rules can override ML scores:

```java
if (ruleDecision == "BLOCK") {
    finalScore = 1.0;  // Force maximum risk
} else if (ruleDecision == "HOLD" && mlScore < 0.7) {
    finalScore = 0.85;  // Force review threshold
}
```

## 4. Anomaly Detection (DL4J Autoencoder)

**Service:** `DL4JAnomalyService`

### 4.1 Architecture

The anomaly detection uses a deep learning autoencoder neural network:

```
Input (20 features) → Encoder (14 neurons) → Bottleneck (8 neurons) → Decoder (14 neurons) → Output (20 features)
```

### 4.2 Anomaly Score Calculation

1. **Feature Normalization:** All features normalized to [0, 1] range
2. **Forward Pass:** Transaction features passed through autoencoder
3. **Reconstruction Error:** Mean Squared Error (MSE) between input and reconstructed output
4. **Anomaly Detection:** If reconstruction error > threshold (default: 0.5), transaction flagged as anomaly

### 4.3 Anomaly Score Formula

```
Anomaly Score = MSE(input, reconstruction)
             = mean((input[i] - reconstruction[i])²) for i in [0, 19]
```

### 4.4 Anomaly Impact

- **High Anomaly Score (> 0.95):** Novel pattern detected, may require enhanced review
- **Anomaly Flag:** Added to risk details but doesn't directly modify ML score
- **Use Case:** Detects patterns not seen in training data

## 5. Score Aggregation and Final Score

**Service:** `ScoringService.scoreTransaction()`

### 5.1 Score Calculation Flow

```
1. Check cache → Return cached score if available
2. Run scheme simulators → Enrich features
3. Call XGBoost ML service → Get base ML score
4. Evaluate Drools rules → Apply rule overrides
5. Run DL4J anomaly detection → Add anomaly flags
6. Cache final result → Store in Aerospike
7. Return ScoringResult
```

### 5.2 Final Score Components

The `ScoringResult` contains:

- **`score`:** Final risk score (0.0 to 1.0)
- **`riskDetails`:** Map containing:
  - `ml_score`: Original ML model score
  - `rule_decision`: Rule engine decision (BLOCK/HOLD/ALLOW)
  - `rules_triggered`: List of triggered rule IDs
  - `rule_reasons`: Explanations for rule triggers
  - `sar_required`: Boolean flag for SAR filing requirement
  - `anomaly_score`: DL4J anomaly detection score
  - `is_anomaly`: Boolean anomaly flag
  - `anomaly_reason`: Anomaly detection explanation
  - `requires_enhanced_review`: Flag for novel patterns
  - Scheme simulator results (VFMP/HECM stages and metrics)

## 6. Decision Engine

**Service:** `DecisionEngine`

The decision engine converts scores into actionable decisions.

### 6.1 Decision Thresholds

Thresholds are configurable via `ConfigService`:

- **Block Threshold:** Default typically 0.9 (configurable)
- **Hold Threshold:** Default typically 0.7 (configurable)

### 6.2 Decision Logic

```java
if (score >= blockThreshold) {
    decision = "BLOCK";
} else if (score >= holdThreshold) {
    decision = "HOLD";
} else {
    decision = "ALLOW";
}
```

### 6.3 Hard Rules (Pre-Scoring)

Before scoring, hard rules are checked:

1. **Sanctions Screening:** Real-time sanctions list check
   - If match found → `DECISION = BLOCK`, `SCORE = 1.0`
   
2. **Blacklist Checks:** PAN and terminal blacklist verification
   - If blacklisted → `DECISION = BLOCK`

### 6.4 AML Rule Integration

After scoring, AML-specific rules are evaluated:

- **High Value Threshold:** Single transaction ≥ threshold → Escalate to ALERT
- **Cumulative Threshold:** 30-day cumulative ≥ 10x single threshold → Escalate to ALERT

## 7. Additional Scoring Systems

### 7.1 Fraud Detection Scoring

**Service:** `FraudDetectionService`

Separate scoring system for fraud-specific risk:

- **Device Risk:** Missing device fingerprint (+10 points)
- **IP Risk:** Missing IP address (+10 points)
- **Behavioral Risk:** Unusual patterns
- **Velocity Risk:** High transaction frequency

**Score Range:** 0-100 points
**Risk Levels:**
- LOW: < 70% of threshold
- MEDIUM: 70-100% of threshold
- HIGH: ≥ threshold (default: 70)

### 7.2 AML Risk Scoring

**Service:** `AmlService`

AML-specific risk scoring:

- **Amount Risk:**
  - Large amount (> $10,000): +20 points
  - Very large amount (> $50,000): +30 points
  
- **Velocity Risk:**
  - Merchant velocity > 50 txns/hour: +15 points
  - Merchant volume > $100K/24h: +20 points
  - PAN velocity > 10 txns/hour: +20 points
  - PAN cumulative > $500K/30d: +25 points
  
- **Geographic Risk:**
  - Cross-border transaction: +15 points

**Score Range:** 0-100+ points
**Risk Levels:**
- LOW: < 30 points
- MEDIUM: 30-60 points
- HIGH: ≥ 80 points

### 7.3 Customer Risk Profiling

**Service:** `CustomerRiskProfilingService`

Calculates customer/merchant risk ratings:

**Risk Score Calculation:**
```java
riskScore = 0.0;

// Case-based risk
if (caseCount > 0) {
    riskScore += min(caseCount * 0.2, 0.5);  // Max 0.5 from cases
}
if (highPriorityCases > 0) {
    riskScore += min(highPriorityCases * 0.3, 0.4);  // Max 0.4 from high priority
}

// Transaction-based risk
if (totalAmount > highValueThreshold) {
    riskScore += 0.3;
}
```

**Risk Level Assignment:**
- HIGH: riskScore ≥ 0.7
- MEDIUM: riskScore ≥ 0.4
- LOW: riskScore < 0.4

**EDD Trigger:** EDD required if riskScore ≥ 0.7 OR riskLevel == "HIGH"

## 8. Configuration Parameters

### 8.1 Scoring Service Configuration

```properties
# Scoring Service
scoring.service.enabled=true
scoring.service.url=http://localhost:8000
scoring.service.retry.max=3
scoring.cache.enabled=true
```

### 8.2 Fraud Detection Configuration

```properties
# Fraud Detection
fraud.enabled=true
fraud.scoring.enabled=true
fraud.scoring.threshold=70
fraud.velocity.checkEnabled=true
fraud.velocity.windowMinutes=60
fraud.velocity.maxTransactions=10
```

### 8.3 AML Configuration

```properties
# AML Configuration
aml.enabled=true
aml.risk.low=30
aml.risk.medium=60
aml.risk.high=80
```

### 8.4 Decision Engine Configuration

```properties
# Decision Thresholds (via ConfigService/DB)
fraud.block.threshold=0.9
fraud.hold.threshold=0.7
aml.high-value.threshold=10000
```

### 8.5 Anomaly Detection Configuration

```properties
# DL4J Anomaly Detection
dl4j.enabled=true
dl4j.anomaly.threshold=0.5
```

### 8.6 Risk Profiling Configuration

```properties
# Customer Risk Profiling
risk.edd.threshold=0.7
risk.high-value.threshold=50000
```

## 9. Score Caching Strategy

### 9.1 Aerospike Cache

Scores are cached in Aerospike for performance:

- **Cache Key:** Transaction ID
- **Cache Value:** Score, latency, risk details
- **Cache TTL:** Configurable (typically 1 hour)
- **Cache Invalidation:** On transaction updates or manual invalidation

### 9.2 Cache Benefits

- **Performance:** Sub-millisecond score retrieval for repeated queries
- **Cost Reduction:** Avoids redundant ML model calls
- **Audit Trail:** Cached results stored for compliance

## 10. Score Interpretation Guide

### 10.1 ML Score Interpretation

| Score Range | Risk Level | Action | Description |
|------------|------------|--------|-------------|
| 0.0 - 0.3 | Low | ALLOW | Normal transaction patterns |
| 0.3 - 0.7 | Medium | ALLOW/HOLD | Some risk indicators present |
| 0.7 - 0.9 | High | HOLD | Requires manual review |
| 0.9 - 1.0 | Very High | BLOCK | Strong fraud indicators |

### 10.2 Combined Score Interpretation

When multiple scoring systems are active:

1. **ML Score** provides baseline risk assessment
2. **Rule Engine** can override with regulatory requirements
3. **Anomaly Detection** flags novel patterns
4. **Final Decision** uses highest risk level

### 10.3 Risk Factors

Common risk factors that contribute to higher scores:

- High transaction amounts
- Unusual velocity patterns
- Cross-border transactions
- High-risk countries
- Graph centrality indicators
- Scheme simulator warnings
- Anomalous patterns

## 11. Performance Considerations

### 11.1 Latency Optimization

- **Caching:** Aerospike cache reduces ML calls
- **Parallel Processing:** Scheme simulators run in parallel
- **Connection Pooling:** REST client uses connection pooling
- **Async Processing:** Optional async orchestrator for high throughput

### 11.2 Typical Latencies

- **Cache Hit:** < 1ms
- **ML Scoring:** 50-200ms (depending on model complexity)
- **Rule Evaluation:** 5-20ms
- **Anomaly Detection:** 10-50ms
- **Total Pipeline:** 100-300ms (without cache)

## 12. Monitoring and Metrics

### 12.1 Key Metrics

- Score distribution histogram
- Average score by merchant/card
- Rule trigger frequency
- Anomaly detection rate
- Cache hit ratio
- Scoring latency percentiles

### 12.2 Alerting

- High score rate anomalies
- ML service failures
- Rule engine errors
- Cache failures
- Performance degradation

## 13. Model Retraining and Updates

### 13.1 ML Model Updates

- XGBoost model retrained periodically on new transaction data
- Model versioning tracked for audit purposes
- A/B testing support for model comparison

### 13.2 Rule Updates

- Drools rules updated via DRL files
- Hot-reload capability for rule changes
- Rule versioning and audit trail

## 14. Compliance and Audit

### 14.1 Score Audit Trail

All scores and decisions are logged with:
- Transaction ID
- Timestamp
- Score components
- Decision rationale
- Rule triggers
- Model version

### 14.2 Regulatory Reporting

- SAR filing triggers tracked
- CTR reporting flags maintained
- Score explanations available for regulatory review

## 15. Weighted Average Scoring Systems (KRS, TRS, CRA)

In addition to the ML-based scoring, the system implements weighted average scoring methodologies similar to industry-standard AML systems. These provide transparent, explainable risk scores based on configurable risk factors.

### 15.1 KYC Risk Score (KRS)

**Service:** `KycRiskScoreService`

KRS calculates customer profile risk using weighted averages of demographic and business factors. KRS changes slowly over time as customer profile changes.

#### 15.1.1 Business User KRS

**Formula:**
```
KRS = (cReg × w_cReg + directorNAT × w_directorNAT + uboNAT × w_uboNAT + 
       rAGE × w_rAGE + bizDomain × w_bizDomain) / 
      (w_cReg + w_directorNAT + w_uboNAT + w_rAGE + w_bizDomain)
```

**Components:**
- `cReg`: Country of registration risk score (0-100)
- `directorNAT`: Director nationality risk score (0-100)
- `uboNAT`: Ultimate Beneficial Owner (UBO) nationality risk score (0-100)
- `rAGE`: Business age risk score (0-100) - newer businesses have higher risk
- `bizDomain`: Business domain/MCC risk score (0-100)

**Default Weights:**
- `w_cReg`: 0.3
- `w_directorNAT`: 0.25
- `w_uboNAT`: 0.25
- `w_rAGE`: 0.1
- `w_bizDomain`: 0.1

**Risk Score Calculation:**
- Country risk: High-risk country = 80, Normal = 30
- Business age: < 1 year = 80, < 3 years = 60, < 5 years = 40, ≥ 5 years = 20
- Business domain: High-risk MCCs (gambling, money transfer) = 90, Medium-risk = 60, Low-risk = 30

#### 15.1.2 Consumer User KRS

**Formula:**
```
KRS = (cRes × w_cRes + cNat × w_nat + age × w_age) / 
      (w_cRes + w_nat + w_age)
```

**Components:**
- `cRes`: Country of residence risk score (0-100)
- `cNat`: Country of nationality risk score (0-100)
- `age`: Age group risk score (0-100)

**Default Weights:**
- `w_cRes`: 0.5
- `w_nat`: 0.3
- `w_age`: 0.2

**Example Calculation:**
- Indian national, 30-40 years old, residing in Dubai
- cRes (Dubai) = 54, cNat (India) = 44, age (30-40) = 76
- KRS = (54 × 0.5 + 44 × 0.3 + 76 × 0.2) / (0.5 + 0.3 + 0.2) = 53.4 (MEDIUM risk)

#### 15.1.3 Missing Data Handling

When data is missing for a risk factor, the system defaults to assigning a very high risk score (default: 100.0) for that parameter, ensuring conservative risk assessment.

### 15.2 Transaction Risk Score (TRS)

**Service:** `TransactionRiskScoreService`

TRS calculates transaction-specific risk using weighted averages of transaction characteristics. TRS changes corresponding to user activity.

**Formula:**
```
TRS_i = (rORG[i] × w_rORG + rDES[i] × w_rDES + rMET[i] × w_rMET + 
         rMER[i] × w_rMER + rPOMET[i] × w_rPOMET + amount[i] × w_Amount) /
        (w_rORG + w_rDES + w_rMET + w_rMER + w_rPOMET + w_Amount)
```

**Components:**
- `rORG`: Payment origin country risk (0-100)
- `rDES`: Payment destination country risk (0-100)
- `rMET`: Payment method risk (0-100)
- `rMER`: Receiver merchant risk (0-100)
- `rPOMET`: Receiving payment method risk (0-100)
- `amount`: Transaction amount risk (0-100)

**Default Weights:**
- `w_rORG`: 0.2
- `w_rDES`: 0.2
- `w_rMET`: 0.15
- `w_rMER`: 0.2
- `w_rPOMET`: 0.1
- `w_Amount`: 0.15

**Risk Score Calculation:**
- Payment origin/destination: High-risk country = 80-85, Normal = 25-30
- Payment method: Card-not-present = 70, Mobile/Digital wallet = 60, Card-present = 30
- Transaction amount: ≥ $50,000 = 90, ≥ $10,000 = 70, ≥ $1,000 = 50, < $1,000 = 30

### 15.3 Customer Risk Assessment (CRA)

**Service:** `CustomerRiskAssessmentService`

CRA is a dynamic aggregate score that evolves over time based on KRS (baseline) and TRS (transaction activity). This creates an adaptive customer risk profile.

**Formula:**
```
CRA[0] = KRS
CRA[1] = avg(KRS + TRS[1])
CRA[2] = avg(CRA[1] + TRS[2])
CRA[i] = avg(CRA[i-1] + TRS[i])
```

**Process:**
1. Initialize CRA with KRS (baseline customer profile risk)
2. For each new transaction:
   - Calculate TRS for the transaction
   - Update CRA: `CRA[i] = (CRA[i-1] + TRS[i]) / 2`
3. CRA evolves to reflect recent transaction patterns

**Characteristics:**
- **Baseline:** Starts with KRS (customer profile risk)
- **Evolution:** Adapts to transaction activity over time
- **Weighting:** Recent transactions have equal weight (simple moving average)
- **Caching:** CRA cached in Aerospike for fast retrieval

**Example:**
- Initial KRS = 50 (MEDIUM risk)
- Transaction 1: TRS = 70 → CRA = (50 + 70) / 2 = 60 (MEDIUM-HIGH)
- Transaction 2: TRS = 80 → CRA = (60 + 80) / 2 = 70 (HIGH)
- Transaction 3: TRS = 30 → CRA = (70 + 30) / 2 = 50 (MEDIUM)

### 15.4 Integration with Existing Scoring

The weighted average scoring systems (KRS, TRS, CRA) complement the ML-based scoring:

1. **KRS** can be used as a feature in ML models or as a baseline risk indicator
2. **TRS** provides explainable transaction-level risk that can be compared with ML scores
3. **CRA** offers an evolving customer risk profile that can inform transaction decisions

**Configuration:**
```properties
# KYC Risk Score Configuration
kyc.risk.weight.countryRegistration=0.3
kyc.risk.weight.directorNationality=0.25
kyc.risk.weight.uboNationality=0.25
kyc.risk.weight.businessAge=0.1
kyc.risk.weight.businessDomain=0.1
kyc.risk.weight.countryResidence=0.5
kyc.risk.weight.countryNationality=0.3
kyc.risk.weight.ageGroup=0.2
kyc.risk.missingDataScore=100.0

# Transaction Risk Score Configuration
trs.weight.paymentOrigin=0.2
trs.weight.paymentDestination=0.2
trs.weight.paymentMethod=0.15
trs.weight.receiverMerchant=0.2
trs.weight.receivingPaymentMethod=0.1
trs.weight.transactionAmount=0.15
trs.missingDataScore=100.0
trs.amount.threshold.low=1000
trs.amount.threshold.medium=10000
trs.amount.threshold.high=50000

# Customer Risk Assessment Configuration
cra.enabled=true
cra.maxTransactions=100
```

### 15.5 Risk Level Conversion

**Score to Risk Level:**
- **HIGH:** Score ≥ 70
- **MEDIUM:** Score ≥ 40 and < 70
- **LOW:** Score < 40

**Risk Level to Score (for configuration):**
When risk levels are configured with ranges, the system uses the average of the range bounds:
- Example: Medium risk level range 60-80 → Score = (60 + 80) / 2 = 70

## 16. Detailed Score Calculation Formulas

This section provides comprehensive documentation of how each scoring system calculates its scores, including step-by-step formulas, component calculations, and worked examples.

### 16.1 KYC Risk Score (KRS) - Detailed Calculation

#### 16.1.1 Business User KRS Calculation

**Step-by-Step Process:**

1. **Calculate Individual Component Scores:**

   **a) Country of Registration Risk (cReg):**
   ```
   IF countryCode IS NULL OR countryCode IS EMPTY:
       cReg = 100.0  // Missing data = high risk
   ELSE IF countryCode IN high_risk_countries:
       cReg = 80.0   // High-risk country
   ELSE:
       cReg = 30.0   // Normal country
   ```

   **b) Director Nationality Risk (directorNAT):**
   ```
   IF directorCountry IS NULL OR directorCountry IS EMPTY:
       directorNAT = 100.0
   ELSE IF directorCountry IN high_risk_countries:
       directorNAT = 75.0
   ELSE:
       directorNAT = 35.0
   ```

   **c) UBO Nationality Risk (uboNAT):**
   ```
   IF uboCountry IS NULL OR uboCountry IS EMPTY:
       uboNAT = 100.0
   ELSE IF uboCountry IN high_risk_countries:
       uboNAT = 75.0
   ELSE:
       uboNAT = 35.0
   ```

   **d) Business Age Risk (rAGE):**
   ```
   yearsOld = CURRENT_DATE - merchant.createdAt
   
   IF yearsOld < 1:
       rAGE = 80.0   // Very new business
   ELSE IF yearsOld < 3:
       rAGE = 60.0   // New business
   ELSE IF yearsOld < 5:
       rAGE = 40.0   // Established business
   ELSE:
       rAGE = 20.0   // Mature business
   ```

   **e) Business Domain Risk (bizDomain):**
   ```
   IF mcc IS NULL OR mcc IS EMPTY:
       bizDomain = 100.0
   ELSE IF mcc IN ["7995", "7273", "6012"]:  // Gambling, adult services, money transfer
       bizDomain = 90.0   // Very high risk
   ELSE IF mcc IN ["5944", "5732"]:  // Jewelry, electronics
       bizDomain = 60.0   // Medium-high risk
   ELSE:
       bizDomain = 30.0   // Low risk (retail, restaurants, etc.)
   ```

2. **Calculate Weighted Average:**

   ```
   numerator = (cReg × w_cReg) + 
               (directorNAT × w_directorNAT) + 
               (uboNAT × w_uboNAT) + 
               (rAGE × w_rAGE) + 
               (bizDomain × w_bizDomain)
   
   denominator = w_cReg + w_directorNAT + w_uboNAT + w_rAGE + w_bizDomain
   
   KRS = numerator / denominator
   ```

   **Default Weights:**
   - w_cReg = 0.3
   - w_directorNAT = 0.25
   - w_uboNAT = 0.25
   - w_rAGE = 0.1
   - w_bizDomain = 0.1
   - Total = 1.0

3. **Worked Example:**

   **Scenario:** Business registered in Kenya, director from Kenya, UBO from Kenya, 2 years old, MCC 7995 (gambling)

   ```
   cReg = 80.0        (Kenya is high-risk)
   directorNAT = 75.0 (Kenya is high-risk)
   uboNAT = 75.0      (Kenya is high-risk)
   rAGE = 60.0        (2 years old = new business)
   bizDomain = 90.0   (Gambling MCC = very high risk)
   
   numerator = (80.0 × 0.3) + (75.0 × 0.25) + (75.0 × 0.25) + (60.0 × 0.1) + (90.0 × 0.1)
             = 24.0 + 18.75 + 18.75 + 6.0 + 9.0
             = 76.5
   
   denominator = 0.3 + 0.25 + 0.25 + 0.1 + 0.1 = 1.0
   
   KRS = 76.5 / 1.0 = 76.5
   
   Risk Level: HIGH (76.5 ≥ 70)
   ```

#### 16.1.2 Consumer User KRS Calculation

**Step-by-Step Process:**

1. **Calculate Individual Component Scores:**

   **a) Country of Residence Risk (cRes):**
   ```
   IF countryResidence IS NULL OR countryResidence IS EMPTY:
       cRes = 100.0
   ELSE IF countryResidence IN high_risk_countries:
       cRes = 70.0
   ELSE:
       cRes = 30.0
   ```

   **b) Country of Nationality Risk (cNat):**
   ```
   IF countryNationality IS NULL OR countryNationality IS EMPTY:
       cNat = 100.0
   ELSE IF countryNationality IN high_risk_countries:
       cNat = 65.0
   ELSE:
       cNat = 35.0
   ```

   **c) Age Group Risk (age):**
   ```
   IF age IS NULL:
       age = 100.0
   ELSE IF age < 18:
       age = 90.0   // Minors
   ELSE IF age < 25:
       age = 70.0   // Young adults
   ELSE IF age < 30:
       age = 60.0   // Late 20s
   ELSE IF age < 40:
       age = 50.0   // 30s
   ELSE IF age < 50:
       age = 40.0   // 40s
   ELSE IF age < 65:
       age = 30.0   // 50s-60s
   ELSE:
       age = 50.0   // Seniors
   ```

2. **Calculate Weighted Average:**

   ```
   numerator = (cRes × w_cRes) + (cNat × w_nat) + (age × w_age)
   denominator = w_cRes + w_nat + w_age
   KRS = numerator / denominator
   ```

   **Default Weights:**
   - w_cRes = 0.5
   - w_nat = 0.3
   - w_age = 0.2
   - Total = 1.0

3. **Worked Example:**

   **Scenario:** Indian national, 35 years old, residing in Dubai

   ```
   cRes = 30.0  (Dubai is not high-risk)
   cNat = 35.0  (India is not high-risk)
   age = 50.0   (35 years old = 30s age group)
   
   numerator = (30.0 × 0.5) + (35.0 × 0.3) + (50.0 × 0.2)
             = 15.0 + 10.5 + 10.0
             = 35.5
   
   denominator = 0.5 + 0.3 + 0.2 = 1.0
   
   KRS = 35.5 / 1.0 = 35.5
   
   Risk Level: LOW (35.5 < 40)
   ```

### 16.2 Transaction Risk Score (TRS) - Detailed Calculation

#### 16.2.1 Component Score Calculations

**Step-by-Step Process:**

1. **Payment Origin Country Risk (rORG):**
   ```
   IF originCountry IS NULL OR originCountry IS EMPTY:
       rORG = 100.0
   ELSE IF originCountry IN high_risk_countries:
       rORG = 85.0
   ELSE:
       rORG = 30.0
   ```

2. **Payment Destination Country Risk (rDES):**
   ```
   IF destinationCountry IS NULL OR destinationCountry IS EMPTY:
       rDES = 100.0
   ELSE IF destinationCountry IN high_risk_countries:
       rDES = 80.0
   ELSE:
       rDES = 25.0
   ```

3. **Payment Method Risk (rMET):**
   ```
   IF channel IS NULL OR channel IS EMPTY:
       rMET = 100.0
   ELSE IF channel IN ["CARD_NOT_PRESENT", "E_COMMERCE"]:
       rMET = 70.0   // High risk - CNP
   ELSE IF channel IN ["MOBILE", "DIGITAL_WALLET"]:
       rMET = 60.0   // Medium-high risk
   ELSE IF channel IN ["CARD_PRESENT", "POS"]:
       rMET = 30.0   // Lower risk
   ELSE:
       rMET = 50.0   // Default medium risk
   ```

4. **Receiver Merchant Risk (rMER):**
   ```
   IF merchantId IS NULL OR merchantId IS EMPTY:
       rMER = 100.0
   ELSE:
       // TODO: Lookup merchant risk profile
       rMER = 50.0   // Default medium risk
   ```

5. **Receiving Payment Method Risk (rPOMET):**
   ```
   IF channel IS NULL OR channel IS EMPTY:
       rPOMET = 100.0
   ELSE IF channel IN ["CARD_NOT_PRESENT", "E_COMMERCE"]:
       rPOMET = 65.0
   ELSE IF channel IN ["MOBILE", "DIGITAL_WALLET"]:
       rPOMET = 55.0
   ELSE IF channel IN ["CARD_PRESENT", "POS"]:
       rPOMET = 35.0
   ELSE:
       rPOMET = 45.0
   ```

6. **Transaction Amount Risk (amount):**
   ```
   IF amountCents IS NULL:
       amount = 100.0
   ELSE:
       amountUSD = amountCents / 100.0
       
       IF amountUSD >= 50000:
           amount = 90.0   // Very high amount
       ELSE IF amountUSD >= 10000:
           amount = 70.0   // High amount
       ELSE IF amountUSD >= 1000:
           amount = 50.0   // Medium amount
       ELSE:
           amount = 30.0   // Low amount
   ```

#### 16.2.2 TRS Formula Application

**Calculate Weighted Average:**

```
numerator = (rORG × w_rORG) + 
            (rDES × w_rDES) + 
            (rMET × w_rMET) + 
            (rMER × w_rMER) + 
            (rPOMET × w_rPOMET) + 
            (amount × w_Amount)

denominator = w_rORG + w_rDES + w_rMET + w_rMER + w_rPOMET + w_Amount

TRS = numerator / denominator
```

**Default Weights:**
- w_rORG = 0.2
- w_rDES = 0.2
- w_rMET = 0.15
- w_rMER = 0.2
- w_rPOMET = 0.1
- w_Amount = 0.15
- Total = 1.0

#### 16.2.3 Worked Example

**Scenario:** Transaction from Kenya to UAE, E-commerce channel, $15,000 amount

```
rORG = 85.0      (Kenya is high-risk origin)
rDES = 25.0      (UAE is normal destination)
rMET = 70.0      (E-commerce = card-not-present)
rMER = 50.0      (Default merchant risk)
rPOMET = 65.0    (E-commerce receiving method)
amount = 70.0    ($15,000 = high amount)

numerator = (85.0 × 0.2) + (25.0 × 0.2) + (70.0 × 0.15) + 
            (50.0 × 0.2) + (65.0 × 0.1) + (70.0 × 0.15)
          = 17.0 + 5.0 + 10.5 + 10.0 + 6.5 + 10.5
          = 59.5

denominator = 0.2 + 0.2 + 0.15 + 0.2 + 0.1 + 0.15 = 1.0

TRS = 59.5 / 1.0 = 59.5

Risk Level: MEDIUM (59.5 ≥ 40 and < 70)
```

### 16.3 Customer Risk Assessment (CRA) - Detailed Calculation

#### 16.3.1 Iterative Calculation Process

**Initialization:**
```
CRA[0] = KRS  // Baseline from customer profile
```

**For Each Transaction i (i = 1, 2, 3, ...):**
```
1. Calculate TRS[i] for transaction i
2. Update CRA: CRA[i] = (CRA[i-1] + TRS[i]) / 2
```

**Mathematical Representation:**
```
CRA[0] = KRS
CRA[1] = (KRS + TRS[1]) / 2
CRA[2] = (CRA[1] + TRS[2]) / 2 = ((KRS + TRS[1])/2 + TRS[2]) / 2
CRA[3] = (CRA[2] + TRS[3]) / 2
...
CRA[i] = (CRA[i-1] + TRS[i]) / 2
```

#### 16.3.2 Worked Example

**Scenario:** Merchant with KRS = 50, processing 5 transactions

```
Initial: CRA[0] = 50.0 (KRS)

Transaction 1: TRS[1] = 70.0
  CRA[1] = (50.0 + 70.0) / 2 = 60.0

Transaction 2: TRS[2] = 80.0
  CRA[2] = (60.0 + 80.0) / 2 = 70.0

Transaction 3: TRS[3] = 30.0
  CRA[3] = (70.0 + 30.0) / 2 = 50.0

Transaction 4: TRS[4] = 75.0
  CRA[4] = (50.0 + 75.0) / 2 = 62.5

Transaction 5: TRS[5] = 65.0
  CRA[5] = (62.5 + 65.0) / 2 = 63.75

Final CRA = 63.75
Risk Level: MEDIUM (63.75 ≥ 40 and < 70)
```

**Characteristics:**
- Recent transactions have equal weight (50% each)
- CRA adapts quickly to transaction patterns
- High TRS values increase CRA, low TRS values decrease it
- CRA converges toward average TRS over time

### 16.4 Machine Learning Score (ML Score) - Calculation Process

#### 16.4.1 XGBoost Model Scoring

**Process Flow:**

1. **Feature Extraction:**
   ```
   features = {
       "amount": transaction.amount,
       "log_amount": log(transaction.amount),
       "merchant_id": transaction.merchantId,
       "pan_txn_count_1h": count(transactions in last hour),
       "pan_txn_amount_sum_7d": sum(amounts in last 7 days),
       "vfmp_stage": scheme_simulator.vfmp_stage,
       "hecm_stage": scheme_simulator.hecm_stage,
       "merchant_fraud_rate": scheme_simulator.fraud_rate,
       "merchant_cb_ratio": scheme_simulator.cb_ratio,
       "pageRank": graph.pageRank,
       "betweenness": graph.betweenness,
       ... (20+ features total)
   }
   ```

2. **Model Inference:**
   ```
   POST /score
   {
       "txn_id": 12345,
       "features": {...}
   }
   
   Response:
   {
       "score": 0.75,  // ML score (0.0-1.0)
       "latency_ms": 45
   }
   ```

3. **Score Interpretation:**
   ```
   IF mlScore >= 0.9:
       Risk Level = VERY HIGH
   ELSE IF mlScore >= 0.7:
       Risk Level = HIGH
   ELSE IF mlScore >= 0.3:
       Risk Level = MEDIUM
   ELSE:
       Risk Level = LOW
   ```

#### 16.4.2 ML Score Override Logic

**After ML scoring, rules may override:**

```
IF ruleDecision == "BLOCK":
    finalMLScore = 1.0  // Force maximum risk
ELSE IF ruleDecision == "HOLD" AND mlScore < 0.7:
    finalMLScore = 0.85  // Force review threshold
ELSE:
    finalMLScore = mlScore  // Keep original score
```

### 16.5 Anomaly Detection Score - Detailed Calculation

#### 16.5.1 Autoencoder Architecture

**Network Structure:**
```
Input Layer: 20 features
  ↓
Encoder Layer 1: 14 neurons (ReLU activation)
  ↓
Encoder Layer 2 (Bottleneck): 8 neurons (ReLU activation)
  ↓
Decoder Layer 1: 14 neurons (ReLU activation)
  ↓
Output Layer: 20 features (Sigmoid activation)
```

#### 16.5.2 Anomaly Score Calculation

**Step-by-Step:**

1. **Feature Normalization:**
   ```
   FOR each feature i:
       normalized[i] = (feature[i] - min[i]) / (max[i] - min[i])
       // Clamped to [0, 1] range
   ```

2. **Forward Pass:**
   ```
   input = normalized_features  // Shape: [1, 20]
   encoding = encoder(input)     // Shape: [1, 8]
   reconstruction = decoder(encoding)  // Shape: [1, 20]
   ```

3. **Reconstruction Error (Anomaly Score):**
   ```
   error = input - reconstruction
   squared_error = error²
   anomaly_score = mean(squared_error)  // MSE across all 20 features
   ```

4. **Anomaly Detection:**
   ```
   IF anomaly_score > threshold (default: 0.5):
       is_anomaly = true
   ELSE:
       is_anomaly = false
   ```

#### 16.5.3 Worked Example

**Scenario:** Transaction with unusual feature combination

```
Input features (normalized):
[0.8, 0.2, 0.9, 0.1, 0.7, 0.3, 0.6, 0.4, 0.5, 0.5,
 0.3, 0.7, 0.2, 0.8, 0.1, 0.9, 0.4, 0.6, 0.5, 0.5]

Reconstruction (after autoencoder):
[0.6, 0.3, 0.7, 0.2, 0.5, 0.4, 0.5, 0.5, 0.4, 0.6,
 0.4, 0.6, 0.3, 0.7, 0.2, 0.8, 0.5, 0.5, 0.4, 0.6]

Error = Input - Reconstruction:
[0.2, -0.1, 0.2, -0.1, 0.2, -0.1, 0.1, -0.1, 0.1, -0.1,
 -0.1, 0.1, -0.1, 0.1, -0.1, 0.1, -0.1, 0.1, 0.1, -0.1]

Squared Error:
[0.04, 0.01, 0.04, 0.01, 0.04, 0.01, 0.01, 0.01, 0.01, 0.01,
 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01]

Anomaly Score = mean(squared_error) = 0.015

is_anomaly = false (0.015 < 0.5 threshold)
```

### 16.6 Fraud Detection Score - Detailed Calculation

#### 16.6.1 Component Score Calculation

**Service:** `FraudDetectionService`

**Score Components:**

1. **Device Risk:**
   ```
   IF deviceFingerprint IS NULL OR deviceFingerprint IS EMPTY:
       deviceRisk = 10 points
   ELSE:
       deviceRisk = 0 points
   ```

2. **IP Risk:**
   ```
   IF ipAddress IS NULL OR ipAddress IS EMPTY:
       ipRisk = 10 points
   ELSE:
       ipRisk = 0 points
   ```

3. **Behavioral Risk:**
   ```
   behavioralRisk = 0 points  // Placeholder for future implementation
   ```

4. **Velocity Risk (if enabled):**
   ```
   IF velocity.checkEnabled:
       // Check transaction count in time window
       IF txnCount > maxTransactions:
           velocityRisk = 10 points
       ELSE:
           velocityRisk = 0 points
   ELSE:
       velocityRisk = 0 points
   ```

#### 16.6.2 Fraud Score Aggregation

**Total Fraud Score:**
```
fraudScore = deviceRisk + ipRisk + behavioralRisk + velocityRisk
```

**Risk Level Determination:**
```
threshold = fraudProperties.scoring.threshold  // Default: 70
mediumThreshold = threshold × 0.7  // Default: 49

IF fraudScore >= threshold:
    riskLevel = HIGH
ELSE IF fraudScore >= mediumThreshold:
    riskLevel = MEDIUM
ELSE:
    riskLevel = LOW
```

#### 16.6.3 Worked Example

**Scenario:** Transaction with missing device fingerprint, missing IP, velocity breach

```
deviceRisk = 10 points  (missing device fingerprint)
ipRisk = 10 points      (missing IP address)
behavioralRisk = 0 points
velocityRisk = 10 points (velocity breach detected)

fraudScore = 10 + 10 + 0 + 10 = 30 points

threshold = 70
mediumThreshold = 49

Risk Level: LOW (30 < 49)
```

### 16.7 AML Risk Score - Detailed Calculation

#### 16.7.1 Component Score Calculation

**Service:** `AmlService`

**Score Components:**

1. **Amount Risk:**
   ```
   IF amount > 50000:
       amountRisk = 30 points  // Very large amount
   ELSE IF amount > 10000:
       amountRisk = 20 points  // Large amount
   ELSE:
       amountRisk = 0 points
   ```

2. **Velocity Risk:**
   ```
   merchantVelocity = 0
   panVelocity = 0
   
   // Merchant velocity
   IF merchantCount1h > 50:
       merchantVelocity += 15 points
   IF merchantAmount24h > 100000:
       merchantVelocity += 20 points
   
   // PAN velocity
   IF panCount1h > 10:
       panVelocity += 20 points
   IF panCumulative30d > 500000:
       panVelocity += 25 points
   
   velocityRisk = merchantVelocity + panVelocity
   ```

3. **Geographic Risk:**
   ```
   IF crossBorderTransaction:
       geographicRisk = 15 points
   ELSE:
       geographicRisk = 0 points
   ```

4. **Pattern Risk:**
   ```
   patternRisk = 0 points  // Placeholder for future implementation
   ```

#### 16.7.2 AML Score Aggregation

**Total AML Score:**
```
amlScore = amountRisk + velocityRisk + geographicRisk + patternRisk
```

**Risk Level Determination:**
```
highThreshold = amlProperties.risk.high      // Default: 80
mediumThreshold = amlProperties.risk.medium  // Default: 60
lowThreshold = amlProperties.risk.low        // Default: 30

IF amlScore >= highThreshold:
    riskLevel = HIGH
ELSE IF amlScore >= mediumThreshold:
    riskLevel = MEDIUM
ELSE IF amlScore >= lowThreshold:
    riskLevel = LOW
ELSE:
    riskLevel = LOW
```

#### 16.7.3 Worked Example

**Scenario:** $15,000 transaction, merchant velocity 60 txns/hour, PAN velocity 12 txns/hour, cross-border

```
amountRisk = 20 points      ($15,000 = large amount)
merchantVelocity = 15 points (60 txns/hour > 50)
panVelocity = 20 points      (12 txns/hour > 10)
geographicRisk = 15 points   (cross-border transaction)
patternRisk = 0 points

amlScore = 20 + 15 + 20 + 15 + 0 = 70 points

highThreshold = 80
mediumThreshold = 60

Risk Level: MEDIUM (70 ≥ 60 and < 80)
```

### 16.8 Customer Risk Profiling Score - Detailed Calculation

#### 16.8.1 Component Score Calculation

**Service:** `CustomerRiskProfilingService`

**Score Components:**

1. **Case-Based Risk:**
   ```
   caseRisk = 0.0
   
   IF caseCount > 0:
       caseRisk += min(caseCount × 0.2, 0.5)  // Max 0.5 from cases
   
   IF highPriorityCases > 0:
       caseRisk += min(highPriorityCases × 0.3, 0.4)  // Max 0.4 from high priority
   ```

2. **Transaction-Based Risk:**
   ```
   transactionRisk = 0.0
   
   totalAmount = sum(transaction amounts for merchant)
   
   IF totalAmount > highValueThreshold (default: 50000):
       transactionRisk = 0.3
   ```

#### 16.8.2 Customer Risk Score Aggregation

**Total Risk Score:**
```
riskScore = caseRisk + transactionRisk
```

**Risk Level Determination:**
```
IF riskScore >= 0.7:
    riskLevel = "HIGH"
ELSE IF riskScore >= 0.4:
    riskLevel = "MEDIUM"
ELSE:
    riskLevel = "LOW"
```

#### 16.8.3 Worked Example

**Scenario:** Merchant with 3 cases (1 high priority), total amount $60,000

```
caseCount = 3
highPriorityCases = 1
totalAmount = 60000
highValueThreshold = 50000

caseRisk = min(3 × 0.2, 0.5) + min(1 × 0.3, 0.4)
         = min(0.6, 0.5) + min(0.3, 0.4)
         = 0.5 + 0.3
         = 0.8

transactionRisk = 0.3  (60000 > 50000)

riskScore = 0.8 + 0.3 = 1.1
// Clamped to 1.0 maximum

Risk Level: HIGH (1.0 ≥ 0.7)
```

### 16.9 Score Aggregation and Final Score

#### 16.9.1 Score Combination Logic

When multiple scoring systems are active, scores are combined as follows:

1. **Primary Score:** ML Score (from XGBoost)
2. **Rule Override:** Rules can override ML score
3. **Anomaly Flag:** Anomaly detection adds flags but doesn't directly modify score
4. **Weighted Scores:** KRS, TRS, CRA stored separately for transparency

**Final Score Determination:**
```
IF ruleDecision == "BLOCK":
    finalScore = 1.0  // Maximum risk
ELSE IF ruleDecision == "HOLD" AND mlScore < 0.7:
    finalScore = 0.85  // Force review
ELSE:
    finalScore = mlScore  // Use ML score
```

#### 16.9.2 Score Storage

All scores are stored in `CaseAlert`:
- `mlScore`: Final ML score (after rule overrides)
- `krsScore`: KYC Risk Score
- `trsScore`: Transaction Risk Score
- `craScore`: Customer Risk Assessment
- `anomalyScore`: Anomaly detection score
- `fraudScore`: Fraud detection score
- `amlScore`: AML risk score
- `ruleScore`: Rule-based score override
- `riskDetailsJson`: Complete risk context

### 16.10 Score Calculation Summary Table

| Score Type | Range | Formula Type | Components | Weighted? |
|------------|-------|--------------|------------|-----------|
| **KRS (Business)** | 0-100 | Weighted Average | 5 factors | Yes |
| **KRS (Consumer)** | 0-100 | Weighted Average | 3 factors | Yes |
| **TRS** | 0-100 | Weighted Average | 6 factors | Yes |
| **CRA** | 0-100 | Iterative Average | KRS + TRS sequence | Yes |
| **ML Score** | 0.0-1.0 | XGBoost Model | 20+ features | Model-learned |
| **Anomaly Score** | 0.0-1.0 | MSE (Reconstruction Error) | 20 features | No |
| **Fraud Score** | 0-100+ | Additive Points | 4 components | No |
| **AML Score** | 0-100+ | Additive Points | 4 components | No |
| **Customer Risk** | 0.0-1.0 | Additive with Caps | 2 components | No |

### 16.11 Missing Data Handling

**Default Behavior:**
- Missing data defaults to **very high risk** (100.0 for 0-100 scale, 1.0 for 0-1 scale)
- Ensures conservative risk assessment
- Logs missing data for investigation

**Example:**
```
IF countryCode IS NULL:
    countryRisk = 100.0  // Conservative assumption
```

## 17. Score Calculation Quick Reference

### 17.1 Formula Summary

**KYC Risk Score (Business):**
```
KRS = Σ(componentScore × weight) / Σ(weights)
Components: cReg, directorNAT, uboNAT, rAGE, bizDomain
```

**KYC Risk Score (Consumer):**
```
KRS = (cRes × 0.5 + cNat × 0.3 + age × 0.2) / 1.0
```

**Transaction Risk Score:**
```
TRS = (rORG × 0.2 + rDES × 0.2 + rMET × 0.15 + rMER × 0.2 + 
       rPOMET × 0.1 + amount × 0.15) / 1.0
```

**Customer Risk Assessment:**
```
CRA[0] = KRS
CRA[i] = (CRA[i-1] + TRS[i]) / 2
```

**Anomaly Score:**
```
anomaly_score = MSE(input_features, reconstructed_features)
              = mean((input[i] - reconstruction[i])²)
```

**Fraud Score:**
```
fraudScore = deviceRisk + ipRisk + behavioralRisk + velocityRisk
```

**AML Score:**
```
amlScore = amountRisk + velocityRisk + geographicRisk + patternRisk
```

**Customer Risk Profiling:**
```
riskScore = min(caseCount × 0.2, 0.5) + 
            min(highPriorityCases × 0.3, 0.4) + 
            (totalAmount > threshold ? 0.3 : 0.0)
```

### 17.2 Component Score Lookup Tables

#### Country Risk Scores

| Country Type | KRS cReg | KRS cRes/cNat | TRS rORG | TRS rDES |
|--------------|----------|---------------|----------|----------|
| High-Risk | 80.0 | 65-70 | 85.0 | 80.0 |
| Normal | 30.0 | 30-35 | 30.0 | 25.0 |
| Missing | 100.0 | 100.0 | 100.0 | 100.0 |

#### Business Age Risk Scores

| Age Range | Risk Score |
|-----------|------------|
| < 1 year | 80.0 |
| 1-3 years | 60.0 |
| 3-5 years | 40.0 |
| ≥ 5 years | 20.0 |

#### Age Group Risk Scores (Consumer)

| Age Range | Risk Score |
|-----------|------------|
| < 18 | 90.0 |
| 18-25 | 70.0 |
| 25-30 | 60.0 |
| 30-40 | 50.0 |
| 40-50 | 40.0 |
| 50-65 | 30.0 |
| ≥ 65 | 50.0 |

#### Payment Method Risk Scores

| Channel Type | Payment Method (rMET) | Receiving Method (rPOMET) |
|--------------|----------------------|---------------------------|
| Card-Not-Present/E-Commerce | 70.0 | 65.0 |
| Mobile/Digital Wallet | 60.0 | 55.0 |
| Card-Present/POS | 30.0 | 35.0 |
| Default | 50.0 | 45.0 |

#### Transaction Amount Risk Scores

| Amount Range | Risk Score |
|--------------|------------|
| ≥ $50,000 | 90.0 |
| $10,000 - $49,999 | 70.0 |
| $1,000 - $9,999 | 50.0 |
| < $1,000 | 30.0 |

#### Business Domain (MCC) Risk Scores

| MCC Category | Examples | Risk Score |
|--------------|----------|------------|
| Very High Risk | 7995 (gambling), 7273 (adult), 6012 (money transfer) | 90.0 |
| Medium-High Risk | 5944 (jewelry), 5732 (electronics) | 60.0 |
| Low Risk | Retail, restaurants, general services | 30.0 |

### 17.3 Risk Level Thresholds

| Score Type | LOW | MEDIUM | HIGH |
|------------|-----|--------|------|
| KRS/TRS/CRA | < 40 | 40-69 | ≥ 70 |
| ML Score | < 0.3 | 0.3-0.69 | ≥ 0.7 |
| Anomaly Score | < 0.5 | 0.5-0.94 | ≥ 0.95 |
| Fraud Score | < 49 | 49-69 | ≥ 70 |
| AML Score | < 30 | 30-79 | ≥ 80 |
| Customer Risk | < 0.4 | 0.4-0.69 | ≥ 0.7 |

### 17.4 Calculation Order

When processing a transaction, scores are calculated in this order:

1. **KRS** - Customer profile risk (cached, calculated once per merchant)
2. **Feature Extraction** - Extract transaction features
3. **ML Score** - XGBoost model scoring
4. **TRS** - Transaction-specific risk
5. **Anomaly Score** - Deep learning anomaly detection
6. **Rule Evaluation** - Drools rules (may override ML score)
7. **Fraud Score** - Fraud detection components
8. **AML Score** - AML risk components
9. **CRA Update** - Update customer risk assessment
10. **Final Score** - Determine final score with overrides

## Conclusion

The scoring process combines multiple sophisticated techniques to provide comprehensive risk assessment:

1. **Feature Engineering:** Rich feature extraction from multiple data sources
2. **Machine Learning:** XGBoost model for pattern recognition
3. **Rule-Based Logic:** Deterministic regulatory compliance
4. **Anomaly Detection:** Deep learning for novel pattern detection
5. **Weighted Average Scoring:** Transparent KRS, TRS, and CRA calculations
6. **Score Aggregation:** Intelligent combination of all signals
7. **Decision Making:** Configurable thresholds for actionable outcomes

This multi-layered approach ensures both accuracy and regulatory compliance while maintaining high performance for real-time transaction processing. The weighted average systems provide explainable risk scores that complement ML-based scoring and offer transparency for regulatory review.

### Key Takeaways

- **Transparency:** All score calculations are documented and explainable
- **Configurability:** Weights and thresholds are configurable via properties
- **Completeness:** All scoring components are tracked and stored
- **Traceability:** Scores are stored in database, metrics, and Kafka events
- **Observability:** Scores visible in Prometheus and Grafana dashboards
