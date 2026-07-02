# CBK (Central Bank of Kenya) Reporting

## Overview

Automated regulatory reporting for PSPs licensed by the Central Bank of Kenya via the GDI (Gateway Data Interface). The platform handles all 17 required endpoints with data sourced entirely from live database queries.

## 17 GDI Endpoints

### Annual (January 4-5)

| # | Endpoint | Scheduled | Data Source |
|---|---|---|---|
| 1 | Senior Management Schedule | Jan 5 | `PspSeniorManagement` table |
| 2 | Schedule of Directors | Jan 5 | `PspDirector` table |
| 3 | Schedule of Trustees | Jan 5 | `PspTrustee` table |
| 4 | Schedule of Shareholders | Jan 4 | `PspShareholder` table |

### Monthly (Days 1-3)

| # | Endpoint | Scheduled | Data Source |
|---|---|---|---|
| 5 | Customer Complaints | Day 3 | `PspCustomerComplaint` table |
| 10 | Products Info | Day 1 | `PspProduct` table |
| 12 | Card Brands | Day 2 | TransactionRepository GROUP BY |
| 14 | Transaction Details | Day 2 | TransactionRepository aggregation |
| 15 | Transaction Tariffs | Day 1 | `PspTariffTemplate` table |

### Daily (2AM)

| # | Endpoint | Data Source |
|---|---|---|
| 6 | Cybersecurity Incident | `PspCyberIncident` table |
| 7 | Fraud/Theft/Robbery Incidents | `PspFraudIncident` table (auto-populated from alerts) |
| 8 | System Stability & Interruption | `PspSystemInterruption` table |
| 9 | System Activity (24h TPS/TPH) | TransactionRepository hourly aggregation |
| 11 | Trust Account | `PspTrustAccount` table |
| 13 | Billing Template | TransactionRepository aggregation |
| 16 | Merchant Transactions (successful) | TransactionRepository yesterday filter |
| 17 | Failed/Rejected Transactions | TransactionRepository failed filter |

## Submission Pipeline

### Scheduling

| Cadence | Cron |
|---|---|
| Daily | @Scheduled(cron = "0 0 2 * * ?") |
| Monthly | Days 1-3, spread across 3 jobs |
| Annual | January 4-5 |

### Orchestration

```
CbkScheduler.triggerReporting()
    │ Resolves ALL PSPs with cbkReportingEnabled = true
    │ For each PSP, dispatches to CbkSubmissionOrchestrator
    ▼
CbkSubmissionOrchestrator.dispatch(pspId, endpointType)
    │ 1. Determines reporting window (DAILY/MONTHLY/ANNUAL)
    │ 2. Queries data source (repository or EntityManager)
    │ 3. Transforms to CBK-required format
    │ 4. Submits via CbkGdiClient
    │ 5. Records CbkSubmission record
    ▼
CbkGdiClient.submit(pspId, endpointType, jsonPayload)
    │ OAuth2 token from CbkTokenService
    │ Multipart HTTP POST to CBK API URL
    │ Circuit breaker (5s timeout, 3 retries)
    │ Logs submission success/failure
    ▼
CbkSubmission(entity)
    │ pspId, endpointType, status (PENDING/SUBMITTED/SUCCESS/FAILED)
    │ requestId, recordCount, attemptedAt
    │ errorMessage on failure
```

### Fraud Incident Auto-Population

When an alert is resolved as TRUE_POSITIVE (BLOCKED, SAR_FILED, REPORTED):

```
AlertFraudIncidentBridge → PspFraudIncident created
    ├── pspId resolved from merchant
    ├── fraudCategoryFlag from disposition type
    ├── alertIdLink for audit trail
    ├── dateOfOccurrence, reportingDate = today
    └── victimInformation from merchant
```

This feeds CBK GDI endpoint #7 (FRAUD_INCIDENTS) with automatically generated records — no manual entry needed.

## Live/Preprod Promotion

3-guard lock:

```
Global flag:  cbk.allow-live (platform config)
Per-PSP flag: psp.cbkAllowLive (PSP-level)
Per-PSP env:  psp.cbkEnvironment = "live" (not "preprod")
```

All three must be true for submissions to reach the live CBK API.

## CBK Configuration

**Admin**:
```http
PUT /psps/{id}/cbk-config
{
  "cbkInstitutionCode": "PSP001",
  "cbkClientId": "client_123",
  "cbkClientSecret": "***",
  "cbkEnvironment": "preprod" | "live",
  "cbkAllowLive": false,
  "cbkReportingEnabled": true
}
```

**PSP**:
```http
GET /psps/{id}/cbk-config  → read-only view
```

## Page Features

**CbkReportingTab** (PSP Config): View/configure CBK settings, environment promotion (admin only).

**CbkSubmissionsTab** (Regulatory Reports): Filterable paginated history with replay capability.

## CBK API Inventory

Full documentation of all CBK GDI endpoints is in `docs/integrations/CBK_API_INVENTORY.md`.