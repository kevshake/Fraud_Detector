# Transaction Monitoring

## Overview

Real-time transaction ingestion and risk scoring pipeline. Every transaction submitted by a PSP is scored against 80+ features across ML models, rules engines, and cache layers to produce a decision: ALLOW, HOLD, BLOCK, or ALERT.

## Architecture

```
PSP System
    │ POST /api/v1/transactions/ingest
    ▼
TransactionIngestionService
    │ 1. Resolves PSP from merchant association
    │ 2. Calculates KRS (Know Risk Score) from merchant profile
    │ 3. Calculates TRS (Transaction Risk Score) from GeoIP + BIN
    │ 4. Updates CRA (Customer Rolling Average)
    │ 5. Publishes to Kafka topic "transactions.raw"
    ▼
FraudDetectionOrchestrator
    │ 1. FeatureExtractionService (80+ features)
    │ 2. Assesses device, IP, velocity, behavioral risk
    │ 3. Publishes to Kafka topics for async processing
    ▼
ScoringService
    │ L1: Aerospike cache (microservice) — sub-ms
    │ L2: Redis cache — ms
    │ L3: XGBoost model — real-time scoring
    │ L4: Drools rules engine — business rules
    │ L5: DL4J anomaly detection — deep learning
    ▼
DecisionEngine
    │ 1. Hard rules: Sanctions screening → Cross-PSP fraud → Blacklists
    │ 2. ML score threshold: BLOCK(≥0.8) / HOLD(≥0.5) / ALLOW
    │ 3. AML rules: Country risk, velocity, amount thresholds
    │ 4. Creates Alert if score ≥ alert threshold
    │ 5. Publishes Kafka event on alert creation
    ▼
Response: { decision, riskScore, riskLevel, triggeredRules }
```

## Key Features

### Batch Scoring

```http
POST /api/v1/transactions/batch-score
Content-Type: application/json

{
  "transactions": [
    { "amount": 12500.00, "currency": "KES", "merchantId": "MRC-001", ... },
    ...
  ]
}
```

### Decision Values

| Decision | Score Range | Meaning |
|---|---|---|
| ALLOW | 0.0 – 0.4 | No suspicious signals |
| HOLD | 0.4 – 0.7 | Additional verification recommended |
| BLOCK | 0.7 – 1.0 | High risk or hard rule triggered |
| ALERT | Any | Flagged for manual review |

### Risk Dimensions

| Dimension | Weight | Source |
|---|---|---|
| Amount Risk | 20% | Transaction amount vs thresholds |
| KRS (Know Risk Score) | 25% | Merchant profile risk |
| TRS (Transaction Risk Score) | 25% | Geographic + channel risk |
| Geographic Risk | 15% | Country risk from DB/config |
| Velocity Risk | 15% | Sliding window transaction count |

### Performance

| Layer | Latency | Hit Rate |
|---|---|---|
| L1: Aerospike (microservice) | <1ms | 40% (repeat merchants) |
| L2: Redis | 1-3ms | 20% (recently scored) |
| L3: XGBoost | 5-15ms | 100% (fallback) |
| L4: Drools | 2-5ms | 100% (always evaluated) |
| L5: DL4J | 20-50ms | 10% (suspicious only) |

## Page Features

**TransactionMonitoringLive**: Real-time feed of scored transactions with decision badges, stats cards (total, approved, declined, manual review), and recent activity sidebar.

**TransactionMonitoringReports**: Aggregate statistics — total volume, average transaction, risk breakdown (high/medium/low), fraud alerts count.

**TransactionMonitoringAnalytics**: Risk distribution doughnut chart, top risk indicators with counts.

**TransactionMonitoringSars**: Transaction-related SAR report management — create, view, and track SAR filings linked to transactions.