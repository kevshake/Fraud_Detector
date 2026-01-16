# Score Tracking Implementation Guide

## Overview

This document describes the comprehensive score tracking implementation that captures all scoring components (KRS, TRS, CRA, ML, Anomaly, Fraud, AML) across cases, transactions, Kafka events, Prometheus metrics, and Grafana dashboards.

## Implementation Summary

### 1. Database Schema Updates

**Migration:** `V102__add_score_fields_to_case_alerts.sql`

Added score fields to `case_alerts` table:
- `ml_score` - Machine Learning score (0.0-1.0)
- `krs_score` - KYC Risk Score (0-100)
- `trs_score` - Transaction Risk Score (0-100)
- `cra_score` - Customer Risk Assessment (0-100)
- `anomaly_score` - Anomaly detection score (0.0-1.0)
- `fraud_score` - Fraud detection score (0-100)
- `aml_score` - AML risk score (0-100)
- `rule_score` - Rule-based score override
- `risk_details_json` - JSON containing all risk details

**Indexes:** Created indexes on all score fields for efficient querying.

### 2. Entity Updates

**CaseAlert Entity** (`src/main/java/com/posgateway/aml/entity/compliance/CaseAlert.java`)

Added fields and getters/setters for all score types:
- All scores stored per alert
- Risk details stored as JSON for flexibility
- Backward compatible (existing `score` field remains as primary score)

### 3. Case Creation Service Updates

**CaseCreationService** (`src/main/java/com/posgateway/aml/service/case_management/CaseCreationService.java`)

Enhanced `createOrUpdateCase()` method:
- Calculates all scores when creating alerts
- Calls `calculateAndStoreAllScores()` method
- Records scores in Prometheus metrics
- Stores scores in database

**Score Calculation Flow:**
1. ML Score - From ScoringService
2. KRS Score - From KycRiskScoreService
3. TRS Score - From TransactionRiskScoreService
4. CRA Score - From CustomerRiskAssessmentService
5. Anomaly Score - Extracted from ML risk details
6. Fraud Score - From FraudDetectionService
7. AML Score - From AmlService
8. Rule Score - From rule evaluation

### 4. Kafka Event Updates

**CaseEventProducer** (`src/main/java/com/posgateway/aml/service/kafka/CaseEventProducer.java`)

Updated `publishCaseLifecycleEvent()`:
- Includes all scores from latest alert
- Scores nested in `scores` object
- Risk details included as nested JSON

**TransactionEventProducer** (`src/main/java/com/posgateway/aml/service/kafka/TransactionEventProducer.java`)

New service for transaction events:
- `publishTransactionEvent()` - Publishes all transaction events with scores
- `publishTransactionAlert()` - Publishes alert events with scores
- Includes ML score, anomaly score, and risk details

**Kafka Topics:**
- `aml.case.lifecycle` - Case events with scores
- `aml.transaction.events` - Transaction events with scores
- `aml.transaction.alerts` - Transaction alerts with scores

### 5. Prometheus Metrics

**PrometheusMetricsService** (`src/main/java/com/posgateway/aml/service/PrometheusMetricsService.java`)

Added score distribution metrics:
- `score_ml_distribution` - ML score distribution histogram
- `score_krs_distribution` - KRS score distribution histogram
- `score_trs_distribution` - TRS score distribution histogram
- `score_cra_distribution` - CRA score distribution histogram
- `score_anomaly_distribution` - Anomaly score distribution histogram
- `score_fraud_distribution` - Fraud score distribution histogram
- `score_aml_distribution` - AML score distribution histogram

**New Method:**
- `recordCaseScores()` - Records all scores from a case alert to Prometheus

### 6. Grafana Dashboard Updates

**Compliance Dashboard** (`grafana/dashboards/04-compliance-dashboard.json`)

Added panels:
- Panel 10: Case Score Distributions (all score types)
- Panel 11: Average Scores by Case Priority
- Panel 12: Score Heatmap

**Transaction Overview Dashboard** (`grafana/dashboards/01-transaction-overview.json`)

Added panels:
- Panel 13: ML Score Distribution
- Panel 14: Weighted Score Distribution (KRS/TRS/CRA)
- Panel 15: Risk Score Distribution (Anomaly/Fraud/AML)

### 7. Fraud Detection Orchestrator Updates

**FraudDetectionOrchestrator** (`src/main/java/com/posgateway/aml/service/FraudDetectionOrchestrator.java`)

Enhanced to publish transaction events:
- Publishes transaction events to Kafka after processing
- Publishes alert events for BLOCK/HOLD/ALERT actions
- Includes all scores in Kafka payloads

## Usage Examples

### Viewing Scores in Cases

When viewing a case, all scores are available:
```java
CaseAlert alert = case.getAlerts().get(0);
Double mlScore = alert.getMlScore();
Double krsScore = alert.getKrsScore();
Double trsScore = alert.getTrsScore();
Double craScore = alert.getCraScore();
// ... etc
```

### Kafka Event Payload Example

```json
{
  "eventType": "CASE_CREATED",
  "caseId": 123,
  "caseReference": "CASE-1234567890",
  "status": "NEW",
  "priority": "HIGH",
  "merchantId": 456,
  "pspId": 1,
  "timestamp": "2024-01-15T10:30:00",
  "scores": {
    "mlScore": 0.85,
    "krsScore": 65.0,
    "trsScore": 72.0,
    "craScore": 68.5,
    "anomalyScore": 0.92,
    "fraudScore": 75.0,
    "amlScore": 80.0,
    "ruleScore": null,
    "primaryScore": 0.85,
    "riskDetails": {
      "ml_score": 0.85,
      "anomaly_score": 0.92,
      "rule_decision": "HOLD",
      "rules_triggered": ["ML_SCORE_MEDIUM_RISK"],
      "krs_risk_level": "MEDIUM",
      "trs_risk_level": "HIGH",
      "cra_risk_level": "HIGH"
    }
  }
}
```

### Prometheus Query Examples

**Average ML Score:**
```promql
avg(score_ml_distribution{psp_code="PSP1"})
```

**Score Distribution Percentiles:**
```promql
histogram_quantile(0.95, sum(rate(score_ml_distribution_bucket[5m])) by (le))
```

**Score by Case Priority:**
```promql
avg(score_ml_distribution) by (priority)
```

### Grafana Dashboard Access

1. **Compliance Dashboard:**
   - Navigate to: Compliance → Cases
   - View score distributions in Panel 10
   - View average scores by priority in Panel 11

2. **Transaction Overview Dashboard:**
   - Navigate to: Transactions → Overview
   - View ML score trends in Panel 13
   - View weighted scores in Panel 14
   - View risk scores in Panel 15

## Kafka Consumer Examples

### Consuming Case Events with Scores

```java
@KafkaListener(topics = "aml.case.lifecycle", groupId = "case-processor")
public void consumeCaseEvent(String message) {
    Map<String, Object> event = objectMapper.readValue(message, Map.class);
    Map<String, Object> scores = (Map<String, Object>) event.get("scores");
    
    Double mlScore = (Double) scores.get("mlScore");
    Double krsScore = (Double) scores.get("krsScore");
    // Process scores...
}
```

### Consuming Transaction Events with Scores

```java
@KafkaListener(topics = "aml.transaction.events", groupId = "txn-processor")
public void consumeTransactionEvent(String message) {
    Map<String, Object> event = objectMapper.readValue(message, Map.class);
    Map<String, Object> scores = (Map<String, Object>) event.get("scores");
    
    Double mlScore = (Double) scores.get("mlScore");
    Map<String, Object> riskDetails = (Map<String, Object>) scores.get("riskDetails");
    // Process transaction with scores...
}
```

## API Endpoints

### Case Details with Scores

When retrieving case details via API, scores are included:
```json
GET /api/cases/{caseId}

Response:
{
  "id": 123,
  "caseReference": "CASE-1234567890",
  "alerts": [
    {
      "id": 1,
      "alertType": "ML_RISK",
      "mlScore": 0.85,
      "krsScore": 65.0,
      "trsScore": 72.0,
      "craScore": 68.5,
      "anomalyScore": 0.92,
      "fraudScore": 75.0,
      "amlScore": 80.0,
      "riskDetailsJson": "{...}"
    }
  ]
}
```

## Performance Considerations

### Database Indexes

All score fields are indexed for fast queries:
- Filter cases by score ranges
- Sort cases by score
- Aggregate scores by merchant/PSP

### Caching

- KRS and CRA scores cached in Aerospike
- Prometheus metrics cached at scrape interval
- Grafana dashboards cache query results

### Latency Impact

- Score calculation adds ~50-150ms per case creation
- Kafka publishing is async (non-blocking)
- Prometheus recording is lightweight (~1ms)

## Monitoring

### Key Metrics to Monitor

1. **Score Distributions:**
   - Average scores by PSP
   - Score percentiles (P50, P95, P99)
   - Score trends over time

2. **Case Scores:**
   - Average scores by case priority
   - Score correlation with case resolution
   - High-score case rate

3. **Transaction Scores:**
   - Score distribution by action (BLOCK/HOLD/ALLOW)
   - Score trends by merchant
   - Anomaly detection rate

### Alerting

Set up alerts for:
- High average scores (> 0.8 ML score)
- Score anomalies (sudden changes)
- Missing scores (data quality)

## Troubleshooting

### Missing Scores

If scores are null:
1. Check service dependencies are injected
2. Verify scoring services are enabled
3. Check logs for calculation errors
4. Verify database migration ran successfully

### Kafka Events Missing Scores

1. Verify case has alerts
2. Check latest alert has scores calculated
3. Verify Kafka producer is working
4. Check Kafka topic configuration

### Prometheus Metrics Not Appearing

1. Verify metrics are being recorded
2. Check Prometheus scrape configuration
3. Verify metric names match dashboard queries
4. Check PSP code tags are set correctly

## Future Enhancements

1. **Score History:** Track score changes over time
2. **Score Explanations:** Add explainability for each score
3. **Score Comparison:** Compare scores across similar cases
4. **Score Drift Detection:** Alert on score distribution changes
5. **Score Validation:** Validate scores against thresholds

## References

- See `SCORING_PROCESS_DOCUMENTATION.md` for scoring algorithm details
- See `WEIGHTED_SCORING_SYSTEMS.md` for KRS/TRS/CRA implementation
- See Prometheus metrics documentation for query syntax
- See Grafana dashboard JSON files for visualization examples
