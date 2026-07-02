# Billing & Pricing

## Overview

The SaaS billing system tracks every API request per PSP, calculates costs based on the selected pricing tier, generates monthly invoices, and supports M-Pesa payments.

## Pricing Tiers

| Tier | Monthly Fee | Per-Check Price | Included Checks | Monthly Minimum |
|---|---|---|---|---|
| Starter | $0 | $0.050 | 1,000 | $0 |
| Growth | $199 | $0.030 | 10,000 | $199 |
| Scale | $499 | $0.020 | 50,000 | $499 |
| Enterprise | $999 | $0.010 | 200,000 | $999 |

## Billing Models

| Model | Calculation | Best For |
|---|---|---|
| PER_REQUEST | `baseRate × requestCount` | Variable usage |
| SUBSCRIPTION | `monthlyFee + max(0, count - included) × overageRate` | Predictable volume |
| TIERED | Cumulative tiers (0-10K @$0.005, 10K-100K @$0.004, etc.) | High volume |

## Billing Pipeline

```
Every API Request
    │
    ├── UsageTrackingFilter (OncePerRequestFilter)
    │   Intercepts /api/v1/* → resolves PSP + service type
    │   Calls ApiUsageTrackingService.logRequest() async
    ▼
    api_usage_log table
    │ psp_id, service_type, request_timestamp, billable, cost
    ▼
Monthly Billing (1st @ 2AM)
    │
    ├── BillingCycleScheduler iterates active subscriptions
    ├── BillingCalculationEngine reads ApiUsageLog + PricingTier
    ├── Computes: base usage + volume discounts + minimums
    └── Creates BillingCalculation record
    ▼
Invoice Generation
    │
    ├── InvoicePdfService (OpenPDF) creates branded A4 PDF
    ├── Line items: per-service breakdown + totals
    └── Invoice status: SENT
    ▼
Email Delivery
    │ BillingEmailService.sendInvoiceEmail()
    │ HTML email with PDF attachment
    │ To: PSP contact email
    ▼
Payment (M-Pesa Daraja)
    │
    ├── PaymentController initiates STK Push
    ├── M-Pesa callback → auto-mark PAID
    └── Invoice status: PAID
    ▼
Dunning (if unpaid)
    │
    ├── DunningScheduler (daily @ 9AM)
    │   → Reminder email (day 7, 14, 21)
    │   → Escalation email (day 30+)
    │   → PSP + platform admin notified
```

## Service Types Tracked

| Service | Tracking Point | Rate |
|---|---|---|
| TRANSACTION_PROCESSING | /transactions/* | Per-check |
| SANCTIONS_SCREENING | /screening/* | Per-check |
| AML_CHECK | /aml/* | Per-check |
| RISK_ASSESSMENT | /risk/* | Per-check |
| REPORT_GENERATION | /reports/generate | Per report |
| CASE_MANAGEMENT | /cases/* | Per operation |
| MERCHANT_ONBOARDING | /merchants | Per merchant |

## Page Features

**BillingPage** (Admin): Revenue Dashboard (current MRR, pending invoices), Subscriptions management, Invoice history, Usage by PSP.

**BillingTab** (PSP): Plan details, current usage against limits, invoice history with PDF downloads.

**SubscriptionController** - Full CRUD:
- GET /subscriptions — list all
- GET /subscriptions/psp/{pspId} — active subscription
- POST /subscriptions — create
- PUT /subscriptions/{id} — update tier
- DELETE /subscriptions/{id} — cancel

## Email Notifications

| Type | Trigger | Content |
|---|---|---|
| Invoice | Monthly generation | HTML + PDF attachment |
| Dunning | 7/14/21 days overdue | Overdue reminder |
| Escalation | 30+ days overdue | Escalation notice (PSP + admin) |
| Usage Alert | Configurable threshold | Usage summary |