# Weighted Average Scoring Systems Implementation Guide

## Overview

This document describes the implementation of weighted average scoring systems (KRS, TRS, CRA) that complement the existing ML-based scoring architecture. These systems provide transparent, explainable risk scores similar to industry-standard AML platforms.

## Architecture

```
┌─────────────────┐
│   KYC Risk      │
│   Score (KRS)   │ ───┐
└─────────────────┘    │
                       │
┌─────────────────┐    │    ┌──────────────────┐
│ Transaction     │ ───┼───▶│ Customer Risk    │
│ Risk Score      │    │    │ Assessment (CRA) │
│ (TRS)           │    │    └──────────────────┘
└─────────────────┘    │
                       │
                       │ CRA[i] = avg(CRA[i-1] + TRS[i])
                       │ CRA[0] = KRS
```

## Services

### 1. KycRiskScoreService

Calculates customer profile risk score using weighted averages.

**Usage:**
```java
@Autowired
private KycRiskScoreService kycRiskScoreService;

// For Business users
KycRiskScoreService.KycRiskScoreResult businessKrs = 
    kycRiskScoreService.calculateBusinessKrs("MERCHANT_123");
double krs = businessKrs.getKrs(); // 0-100
String riskLevel = businessKrs.getRiskLevel(); // LOW, MEDIUM, HIGH

// For Consumer users
KycRiskScoreService.KycRiskScoreResult consumerKrs = 
    kycRiskScoreService.calculateConsumerKrs("AE", "IN", 35);
double krs = consumerKrs.getKrs();
```

**Key Features:**
- Configurable weights for each risk factor
- Missing data defaults to high risk (100.0)
- Supports both Business and Consumer user types
- Transparent calculation with component scores

### 2. TransactionRiskScoreService

Calculates transaction-specific risk score using weighted averages.

**Usage:**
```java
@Autowired
private TransactionRiskScoreService transactionRiskScoreService;

TransactionRiskScoreService.TransactionRiskScoreResult trsResult = 
    transactionRiskScoreService.calculateTrs(transaction);
double trs = trsResult.getTrs(); // 0-100
String riskLevel = trsResult.getRiskLevel();
```

**Key Features:**
- Six weighted risk factors (origin, destination, payment method, merchant, receiving method, amount)
- Configurable thresholds for amount-based risk
- Missing data handling

### 3. CustomerRiskAssessmentService

Calculates evolving customer risk profile based on KRS and TRS.

**Usage:**
```java
@Autowired
private CustomerRiskAssessmentService customerRiskAssessmentService;

// Calculate current CRA
CustomerRiskAssessmentService.CustomerRiskAssessmentResult craResult = 
    customerRiskAssessmentService.calculateCra("MERCHANT_123");
double cra = craResult.getCra();
double krs = craResult.getKrs();
int transactionCount = craResult.getTransactionCount();

// Update CRA with new transaction
CustomerRiskAssessmentService.CustomerRiskAssessmentResult updatedCra = 
    customerRiskAssessmentService.updateCraWithTransaction("MERCHANT_123", newTransaction);
```

**Key Features:**
- Dynamic evolution: CRA adapts to transaction activity
- Baseline: Starts with KRS
- Iterative calculation: Each transaction updates CRA
- Caching: Results cached in Aerospike for performance

## Integration with Existing Scoring

### Option 1: Use as Features

Add KRS, TRS, and CRA as features in the ML model:

```java
Map<String, Object> features = featureExtractionService.extractFeatures(transaction);

// Add weighted scoring features
double krs = kycRiskScoreService.calculateBusinessKrs(merchantId).getKrs();
double trs = transactionRiskScoreService.calculateTrs(transaction).getTrs();
double cra = customerRiskAssessmentService.calculateCra(merchantId).getCra();

features.put("krs", krs);
features.put("trs", trs);
features.put("cra", cra);

// Send to ML model
ScoringResult result = scoringService.scoreTransaction(txnId, features);
```

### Option 2: Use as Standalone Scores

Use weighted scores independently for explainable risk assessment:

```java
// Calculate all scores
double krs = kycRiskScoreService.calculateBusinessKrs(merchantId).getKrs();
double trs = transactionRiskScoreService.calculateTrs(transaction).getTrs();
double mlScore = scoringService.scoreTransaction(txnId, features).getScore();

// Combine or compare
if (trs > 70 && mlScore > 0.7) {
    // High risk from both systems - escalate
}
```

### Option 3: Use CRA for Customer-Level Decisions

Use CRA for customer-level risk decisions (e.g., EDD triggers):

```java
double cra = customerRiskAssessmentService.calculateCra(merchantId).getCra();
if (cra >= 70) {
    // Trigger Enhanced Due Diligence
    eddService.triggerEdd(merchantId);
}
```

## Configuration

### application.properties

```properties
# KYC Risk Score Weights (Business)
kyc.risk.weight.countryRegistration=0.3
kyc.risk.weight.directorNationality=0.25
kyc.risk.weight.uboNationality=0.25
kyc.risk.weight.businessAge=0.1
kyc.risk.weight.businessDomain=0.1

# KYC Risk Score Weights (Consumer)
kyc.risk.weight.countryResidence=0.5
kyc.risk.weight.countryNationality=0.3
kyc.risk.weight.ageGroup=0.2

# Missing Data Default Score
kyc.risk.missingDataScore=100.0

# Transaction Risk Score Weights
trs.weight.paymentOrigin=0.2
trs.weight.paymentDestination=0.2
trs.weight.paymentMethod=0.15
trs.weight.receiverMerchant=0.2
trs.weight.receivingPaymentMethod=0.1
trs.weight.transactionAmount=0.15

# Transaction Risk Score Amount Thresholds
trs.amount.threshold.low=1000
trs.amount.threshold.medium=10000
trs.amount.threshold.high=50000
trs.missingDataScore=100.0

# Customer Risk Assessment
cra.enabled=true
cra.maxTransactions=100
```

## Risk Score Interpretation

### Score Ranges

| Score Range | Risk Level | Description |
|------------|------------|-------------|
| 0-40 | LOW | Low risk profile/transaction |
| 40-70 | MEDIUM | Medium risk, may require review |
| 70-100 | HIGH | High risk, requires attention |

### Missing Data Handling

When data is missing for a risk factor:
- System assigns default high risk score (100.0)
- Ensures conservative risk assessment
- Logs missing data for investigation

## Performance Considerations

### Caching

- KRS: Cached in Aerospike (merchant-level cache)
- TRS: Calculated per transaction (not cached)
- CRA: Cached in Aerospike (updated on new transactions)

### Latency

- KRS calculation: ~5-10ms (with cache: <1ms)
- TRS calculation: ~2-5ms per transaction
- CRA calculation: ~10-50ms (depends on transaction count)

## Example Workflow

### Scenario: New Transaction Processing

```java
// 1. Extract features
Map<String, Object> features = featureExtractionService.extractFeatures(transaction);

// 2. Calculate KRS (if not cached)
double krs = kycRiskScoreService.calculateBusinessKrs(merchantId).getKrs();

// 3. Calculate TRS
double trs = transactionRiskScoreService.calculateTrs(transaction).getTrs();

// 4. Update CRA
double cra = customerRiskAssessmentService.updateCraWithTransaction(merchantId, transaction).getCra();

// 5. Calculate ML score (with weighted scores as features)
features.put("krs", krs);
features.put("trs", trs);
features.put("cra", cra);
double mlScore = scoringService.scoreTransaction(txnId, features).getScore();

// 6. Make decision
if (mlScore > 0.9 || trs > 80 || cra > 75) {
    decisionEngine.evaluate(transaction, mlScore, features);
}
```

## Best Practices

1. **Weight Tuning:** Adjust weights based on business requirements and historical data
2. **Missing Data:** Investigate and fix missing data sources rather than relying on defaults
3. **CRA Evolution:** Monitor CRA trends to identify customers with increasing risk
4. **Score Comparison:** Compare weighted scores with ML scores to identify discrepancies
5. **Documentation:** Document weight choices and rationale for regulatory review

## Future Enhancements

1. **Dynamic Weights:** Adjust weights based on transaction patterns
2. **Time Decay:** Apply time decay to CRA calculation (recent transactions weighted more)
3. **Country Risk Database:** Integrate external country risk databases
4. **Merchant Risk Profiles:** Build merchant-specific risk profiles for rMER calculation
5. **Consumer KRS:** Enhance consumer KRS with additional factors (employment, income, etc.)

## Troubleshooting

### High KRS Scores

- Check country risk database configuration
- Verify business age calculation
- Review MCC risk assignments

### High TRS Scores

- Verify payment method classification
- Check amount thresholds
- Review merchant risk profiles

### CRA Not Updating

- Check CRA service is enabled (`cra.enabled=true`)
- Verify transaction repository queries
- Check Aerospike cache connectivity

## References

- See `SCORING_PROCESS_DOCUMENTATION.md` for complete scoring architecture
- See service JavaDoc for detailed API documentation
- See `application.properties` for configuration options
