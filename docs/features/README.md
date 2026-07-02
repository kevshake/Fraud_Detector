# Hokeka AML Platform — Feature Documentation

> **Version:** 2.0 | **Last Updated:** June 2026

This directory contains detailed documentation for every feature, page, and workflow in the Hokeka AML Platform. Each document covers architecture, data flow, user interaction, and technical implementation.

---

## Core Features

| Document | Description |
|---|---|
| [Transaction Monitoring](TRANSACTION_MONITORING.md) | Real-time transaction ingestion, scoring pipeline, decision engine |
| [Fraud Detection](FRAUD_DETECTION.md) | ML models, rules engine, feature extraction, risk scoring |
| [AML Screening](AML_SCREENING.md) | Sanctions checks, PEP screening, watchlist management, name matching |
| [Alert Management](ALERT_MANAGEMENT.md) | Alert lifecycle, disposition workflow, bulk actions, escalation matrix |
| [Case Management](CASE_MANAGEMENT.md) | Case creation, investigation timeline, decisions, SAR filing, network graphs |
| [Cross-PSP Fraud Intelligence](CROSS_PSP_FRAUD_INTELLIGENCE.md) | Shared fraud flags, multi-tenant blocking, name-based matching |

## Regulatory Compliance

| Document | Description |
|---|---|
| [Sanctions Compliance](SANCTIONS_COMPLIANCE.md) | OFAC, UN, EU sanctions screening, Aerospike cache, fuzzy matching |
| [Regulatory Reporting](REGULATORY_REPORTING.md) | CTR, LCTR, IFTR reports, goAML SAR XML, export (PDF/CSV) |
| [CBK Reporting](CBK_REPORTING.md) | 17 GDI endpoints for Central Bank of Kenya, automated daily/monthly/annual submissions |

## Platform Management

| Document | Description |
|---|---|
| [Billing & Pricing](BILLING_PRICING.md) | SaaS pricing tiers, per-request billing, invoice generation, M-Pesa payments |
| [PSP Management](PSP_MANAGEMENT.md) | Multi-tenant administration, PSP config, CBK credentials, onboarding workflow |
| [User Management](USER_MANAGEMENT.md) | Role-based access control (RBAC), user CRUD, permissions, audit logging |
| [Dashboard & Analytics](DASHBOARD_ANALYTICS.md) | Live KPIs, risk heatmaps, trend charts, compliance health, alerts queue |

## Client Integration

| Document | Description |
|---|---|
| [API Integration Guide](API_INTEGRATION.md) | Authentication, endpoints, SDK examples, webhooks, rate limits |
| [Notifications](NOTIFICATIONS.md) | Email alerts, webhook events, in-app notifications, dunning reminders |

---

## Page Directory

| Page | Route | Feature Doc |
|---|---|---|
| Dashboard | `/dashboard` | [Dashboard & Analytics](DASHBOARD_ANALYTICS.md) |
| Alerts | `/alerts` | [Alert Management](ALERT_MANAGEMENT.md) |
| Cases | `/cases` | [Case Management](CASE_MANAGEMENT.md) |
| Transaction Monitoring | `/monitoring` | [Transaction Monitoring](TRANSACTION_MONITORING.md) |
| Screening | `/screening` | [AML Screening](AML_SCREENING.md) |
| Merchant KYC | `/kyc` | [AML Screening](AML_SCREENING.md) |
| Risk Analytics | `/risk-analytics` | [Dashboard & Analytics](DASHBOARD_ANALYTICS.md) |
| Reports | `/reports` | [Regulatory Reporting](REGULATORY_REPORTING.md) |
| Regulatory Reports | `/regulatory-reports` | [CBK Reporting](CBK_REPORTING.md) |
| Reports Center | `/reports-center` | [Regulatory Reporting](REGULATORY_REPORTING.md) |
| Billing | `/billing` | [Billing & Pricing](BILLING_PRICING.md) |
| PSPs | `/psps` | [PSP Management](PSP_MANAGEMENT.md) |
| Users | `/users` | [User Management](USER_MANAGEMENT.md) |
| Settings | `/settings` | [PSP Management](PSP_MANAGEMENT.md) |
| Messages | `/messages` | [Notifications](NOTIFICATIONS.md) |
| Compliance Calendar | `/compliance-calendar` | [Regulatory Reporting](REGULATORY_REPORTING.md) |
| Audit Logs | `/audit-logs` | [User Management](USER_MANAGEMENT.md) |
| Rules | `/rules` | [Fraud Detection](FRAUD_DETECTION.md) |
| Rules Generation | `/rules-generation` | [Fraud Detection](FRAUD_DETECTION.md) |
| Profile | `/profile` | [User Management](USER_MANAGEMENT.md) |
| Analytics | `/analytics` | [Dashboard & Analytics](DASHBOARD_ANALYTICS.md) |