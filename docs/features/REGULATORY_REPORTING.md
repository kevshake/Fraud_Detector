# Regulatory & CBK Reporting

## Overview

Comprehensive regulatory reporting covering FIU requirements (SAR, CTR, LCTR, IFTR) and Central Bank of Kenya GDI (Gateway Data Interface) submissions.

## Regulatory Reports

### Suspicious Activity Report (SAR)

```http
GET /api/v1/reporting/regulatory/sar
```

Generated via `RegulatoryReportingService`:
- goAML-pattern XML via `FrcReportingService`
- SAR type: INITIAL, FOLLOW_UP, or CANCELLATION
- Includes narrative, transaction details, risk indicators

### Currency Transaction Report (CTR)

```http
GET /api/v1/reporting/regulatory/ctr
GET /api/v1/reporting/regulatory/ctr/export?format=pdf|csv
```

### Large Cash Transaction Report (LCTR)

```http
GET /api/v1/reporting/regulatory/lctr
GET /api/v1/reporting/regulatory/lctr/export?format=pdf|csv
```

### International Funds Transfer Report (IFTR)

```http
GET /api/v1/reporting/regulatory/iftr
GET /api/v1/reporting/regulatory/iftr/export?format=pdf|csv
```

## CBK (Central Bank of Kenya) GDI Reporting

### 17 Endpoints

| # | Endpoint | Cadence | Data Source |
|---|---|---|---|
| 1 | Senior Management Schedule | Annual (Jan 5) | PspSeniorManagement table |
| 2 | Schedule of Directors | Annual (Jan 5) | PspDirector table |
| 3 | Schedule of Trustees | Annual (Jan 5) | PspTrustee table |
| 4 | Schedule of Shareholders | Annual (Jan 4) | PspShareholder table |
| 5 | Customer Complaints | Monthly (Day 3) | PspCustomerComplaint table |
| 6 | Cybersecurity Incident | Daily | PspCyberIncident table |
| 7 | Fraud/Theft/Robbery Incidents | Daily | PspFraudIncident table (auto-populated) |
| 8 | System Stability & Interruption | Daily | PspSystemInterruption table |
| 9 | System Activity (24h TPS/TPH) | Daily | TransactionRepository aggregation |
| 10 | Products Info | Monthly (Day 1) | PspProduct table |
| 11 | Trust Account | Daily | PspTrustAccount table |
| 12 | Card Brands | Monthly (Day 2) | TransactionRepository aggregation |
| 13 | Billing Template | Daily | TransactionRepository aggregation |
| 14 | Transaction Details | Monthly (Day 2) | TransactionRepository aggregation |
| 15 | Transaction Tariffs | Monthly (Day 1) | PspTariffTemplate table |
| 16 | Merchant Transactions (success) | Daily | TransactionRepository aggregation |
| 17 | Failed/Rejected Transactions | Daily | TransactionRepository aggregation |

### Submission Pipeline

```
CbkScheduler (6 cron jobs)
    │ Daily 2AM, Monthly spread Days 1-3, Annual Jan 4-5
    │ Conditional on cbk.enabled global flag
    ▼
CbkSubmissionOrchestrator
    │ 1. Resolves PSPs with cbkReportingEnabled=true
    │ 2. Dispatches to specific endpoint handler
    │ 3. Queries data for the reporting window
    │ 4. Submits via CbkGdiClient
    ▼
CbkGdiClient
    │ OAuth2 authentication (CbkTokenService)
    │ Multipart HTTP POST to CBK API
    │ Circuit breaker (Resilience4j) + retries
    ▼
CbkSubmission
    │ Status: PENDING → SUBMITTED → SUCCESS/FAILED
    │ PSP-scoped submission audit trail
    │ Auto-retry on failure (3 attempts)
```

### Live Lock System

3-guard promotion check:
1. Global `cbk.allow-live` (platform config)
2. Per-PSP `cbkAllowLive` (PSP config)
3. Per-PSP `cbkEnvironment` = "live"

### Fraud Incident Auto-Population

When an alert is resolved TRUE_POSITIVE:
- `AlertFraudIncidentBridge` creates `PspFraudIncident` automatically
- Feeds CBK GDI endpoint #7 (FRAUD_INCIDENTS) with real data
- No manual entry needed for confirmed fraud

## Page Features

**RegulatoryReportsPage**: CTR/LCTR/IFTR tabbed view with:
- Statistics cards (total transactions, total amount, period)
- Transaction detail table (first 20)
- CSV export
- CBK Submissions tab with filterable history

**CbkSubmissionsTab**: Filterable paginated table:
- PSP filter, status filter, endpoint filter
- Status badges (SUCCESS/green, FAILED/red, PENDING/yellow, RETRYING/purple)
- Replay button for failed submissions
- Request ID and record count display

## Report Export

Supported formats per report type:

| Report | JSON | PDF (OpenPDF) | CSV (UTF-8 BOM) |
|---|---|---|---|
| Transaction Summary | ✅ | ✅ | ✅ |
| CTR | ✅ | ✅ | ✅ |
| LCTR | ✅ | ✅ | ✅ |
| IFTR | ✅ | ✅ | ✅ |
| SAR | ✅ | ❌ | ❌ |
| Case Report | ✅ | ✅ | ✅ |
| Alert Report | ✅ | ✅ | ✅ |

Export via: `/reports/download/{id}?format=pdf|csv|xml`