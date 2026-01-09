# Functional Specification Document
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026  
**Status:** Production-Ready

---

## 1. Document Purpose

This Functional Specification Document (FSD) describes the business functions, features, and capabilities of the AML Fraud Detector system. It serves as a reference for stakeholders to understand what the system does from a functional perspective.

---

## 2. System Overview

### 2.1 Business Purpose

The AML Fraud Detector is a comprehensive compliance and risk management platform designed to:

1. **Detect Fraudulent Transactions** - Real-time analysis of payment transactions to identify fraud
2. **Ensure AML/CFT Compliance** - Monitor transactions for money laundering and terrorist financing
3. **Screen Against Sanctions Lists** - Check entities against global watchlists
4. **Manage Compliance Cases** - Track and resolve suspicious activity investigations
5. **Generate Regulatory Reports** - Automate SAR and other compliance reporting

### 2.2 Target Users

| User Role | Primary Functions |
|-----------|-------------------|
| Compliance Officer | Case review, SAR filing, regulatory reporting |
| Fraud Analyst | Transaction monitoring, alert investigation |
| AML Analyst | AML screening, risk assessment |
| System Administrator | User management, configuration |
| Super Admin | Full system access, audit review |

---

## 3. Functional Requirements

### 3.1 Transaction Management

#### F3.1.1 Transaction Ingestion
- **Description**: Receive and process transactions from merchant systems
- **Input**: ISO 8583 messages, transaction details, EMV tags
- **Output**: Transaction ID, fraud score, decision (BLOCK/HOLD/ALLOW)
- **Processing Time**: < 200ms

| Field | Required | Description |
|-------|----------|-------------|
| pan | Yes | Primary Account Number (tokenized) |
| merchantId | Yes | Merchant identifier |
| terminalId | Yes | Terminal identifier |
| amountCents | Yes | Transaction amount in cents |
| currency | Yes | ISO 4217 currency code |
| txnTs | Yes | Transaction timestamp |
| emvTags | No | EMV chip data |

#### F3.1.2 Transaction Monitoring
- Real-time transaction listing with filters
- Search by transaction ID, merchant, date range
- Export capabilities (CSV, PDF)
- Transaction details view with full EMV data

#### F3.1.3 Transaction Analytics
- Daily/weekly/monthly volume trends
- Amount distribution analysis
- Geographic distribution
- Merchant performance metrics

---

### 3.2 Fraud Detection

#### F3.2.1 Real-Time Scoring
- **ML Model Integration**: XGBoost-based fraud scoring
- **Score Range**: 0.0 (legitimate) to 1.0 (fraudulent)
- **Features Used**:
  - Transaction-level (amount, currency, time)
  - Behavioral (velocity, aggregates)
  - EMV-specific (chip presence, CVM method)
  - AML-specific (cumulative amounts, patterns)

#### F3.2.2 Decision Engine Rules

| Rule Type | Description | Action |
|-----------|-------------|--------|
| Hard Block | PAN/Terminal on blacklist | BLOCK |
| Score Block | Score ≥ 0.95 | BLOCK |
| Score Hold | Score ≥ 0.70 | HOLD |
| AML Alert | High-risk pattern detected | ALERT |
| Allow | No triggers | ALLOW |

#### F3.2.3 Feature Extraction
- **Velocity Features**: Transaction count/amount in time windows
- **Behavioral Features**: Distinct terminals, average amounts
- **Pattern Features**: Structuring detection, round numbers
- **Geographic Features**: Cross-border, high-risk country

---

### 3.3 AML/CFT Compliance

#### F3.3.1 Sanctions Screening
- **Screening Engine**: Aerospike-based + Sumsub integration
- **Watchlists Supported**:
  - OFAC SDN List
  - UN Consolidated List
  - EU Sanctions List
  - Custom internal lists
- **Match Types**: Exact, fuzzy (configurable threshold)

#### F3.3.2 Velocity Monitoring
| Check | Threshold | Action |
|-------|-----------|--------|
| Merchant txns/hour | 50+ | Review |
| Merchant amount/24h | $100,000+ | Alert |
| PAN txns/hour | 10+ | Alert |
| PAN cumulative/30d | $500,000+ | Case |

#### F3.3.3 Risk Assessment
- **Risk Factors**:
  - Transaction amount (high-value, structuring)
  - Velocity anomalies
  - Geographic risk (cross-border, high-risk countries)
  - Pattern detection (smurfing, layering)
- **Risk Score**: 0-100 aggregated score

---

### 3.4 Case Management

#### F3.4.1 Case Lifecycle

```
NEW → ASSIGNED → INVESTIGATING → PENDING_REVIEW → RESOLVED/ESCALATED/CLOSED
```

| Status | Description |
|--------|-------------|
| NEW | Case created, awaiting assignment |
| ASSIGNED | Assigned to analyst |
| INVESTIGATING | Active investigation |
| PENDING_REVIEW | Awaiting supervisor review |
| RESOLVED | Investigation complete, no action |
| ESCALATED | Escalated for SAR filing |
| CLOSED | Case closed |

#### F3.4.2 Case Features
- **Timeline View**: Chronological event history
- **Network Graph**: Related entities visualization
- **Document Attachment**: Supporting evidence
- **Notes & Comments**: Investigation documentation
- **SLA Tracking**: Time-based deadline monitoring

#### F3.4.3 Work Queues
- Specialized queues by case type
- Skill-based routing
- Priority-based assignment
- Load balancing across analysts

#### F3.4.4 Case Enrichment
- **Automatic Transaction Linking**: Links triggering and related transactions to cases
- **Merchant Profile Enrichment**: Automatically links merchant profiles and triggers background KYC checks
- **Risk Detail Attachment**: Attaches structured risk assessment details as case notes
- **Graph Context Updates**: Updates Neo4j graph to flag merchants under investigation
- **Async Processing**: Enrichment runs asynchronously to avoid blocking case creation

#### F3.4.5 Case Archival & Retention
- **Archival Status**: Tracks archived status and archival timestamp
- **Cold Storage Integration**: Archive reference points to cold storage (e.g., S3 path/ARN)
- **Retention Policy**: Cases archived after 1 year, retained for 7 years total
- **Compliance**: Meets regulatory data retention requirements

#### F3.4.6 Graph Anomaly Detection
- **Circular Trading Detection**: Identifies money loops (cycles of length 3-6)
- **Mule Proximity Detection**: Detects proximity to high-risk entities (within 3 hops)
- **Risk Status Updates**: Updates merchant risk scores and investigation flags in graph
- **Anomaly Scoring**: Provides anomaly scores for case prioritization

---

### 3.5 Alert Management

#### F3.5.1 Alert Types
| Type | Description | Priority |
|------|-------------|----------|
| HIGH_FRAUD_SCORE | ML score exceeds threshold | High |
| SANCTIONS_MATCH | Entity matches watchlist | Critical |
| VELOCITY_BREACH | Velocity limits exceeded | Medium |
| AML_PATTERN | Suspicious pattern detected | High |
| LARGE_TRANSACTION | Amount exceeds threshold | Medium |
| GRAPH_ANOMALY | Circular trading or mule proximity detected | Critical |
| RULE_VIOLATION | Business rule violation detected | High |
| ML_RISK | High ML risk score | High |

#### F3.5.1.1 Alert Versioning
- **Rule Versioning**: Tracks rule version (e.g., "v1.0") for auditability
- **Model Versioning**: Tracks ML model version used for scoring
- **Alert Traceability**: Full traceability of which rule/model version triggered alert

#### F3.5.2 Alert Workflow
- Automatic creation from detection rules
- Manual investigation and disposition
- Bulk actions (assign, dismiss, escalate)
- Alert aging reports

#### F3.5.3 Alert Tuning
- Disposition statistics analysis
- Threshold recommendation engine
- A/B testing for rule changes

---

### 3.6 Merchant Management

#### F3.6.1 Merchant Onboarding
- Business information capture
- Beneficial ownership disclosure
- KYC document collection
- Sanctions screening

#### F3.6.2 Merchant Profile
| Field | Description |
|-------|-------------|
| Business Name | Legal business name |
| MCC Code | Merchant Category Code |
| Risk Level | LOW/MEDIUM/HIGH |
| KYC Status | PENDING/VERIFIED/REJECTED |
| Contract Status | ACTIVE/SUSPENDED/TERMINATED |
| Daily Limit | Transaction limit |

#### F3.6.3 Merchant Monitoring
- Transaction volume tracking
- Chargeback rates
- Risk score trending
- Periodic re-screening

---

### 3.7 User & Access Management

#### F3.7.1 User Roles
| Role | Permissions |
|------|-------------|
| SUPER_ADMIN | Full system access |
| ADMIN | User/role management, configuration |
| COMPLIANCE_OFFICER | Cases, reporting, SAR filing |
| AML_ANALYST | AML screening, investigation |
| FRAUD_ANALYST | Fraud alerts, transaction review |
| VIEWER | Read-only access |

#### F3.7.2 User Features
- User creation and management
- Role assignment
- Password management
- Activity logging
- Concurrent session control

---

### 3.8 Reporting & Analytics

#### F3.8.1 Standard Reports
| Report | Frequency | Format |
|--------|-----------|--------|
| SAR Report | On-demand | PDF/XML |
| IFTR Report | Daily | Regulatory format |
| Alert Summary | Daily/Weekly | PDF |
| Case Aging | Weekly | PDF |
| Transaction Volume | Daily | PDF |

#### F3.8.2 Dashboard Analytics
- **Transaction Volume Chart**: Daily transaction trends
- **Risk Breakdown**: Distribution by risk level
- **Fraud Metrics**: Detection rates, false positives
- **Case Aging**: Open cases by age
- **Geographic Heatmap**: Risk by country

#### F3.8.3 Compliance Calendar
- Regulatory deadline tracking
- Automated reminders
- Deadline management

---

### 3.9 System Configuration

#### F3.9.1 Configurable Parameters
| Category | Examples |
|----------|----------|
| Fraud Thresholds | Block threshold, hold threshold |
| AML Thresholds | High-value amount, velocity limits |
| Screening | Fuzzy match threshold, watchlist selection |
| Notifications | Email/Slack configuration |
| Session | Timeout, max concurrent |

#### F3.9.2 High-Risk Country Management
- Add/remove countries from high-risk list
- Configure risk factors per country
- Audit trail for changes

---

## 4. Non-Functional Requirements

### 4.1 Performance
- Transaction processing: < 200ms P99
- Concurrent users: 1,000+
- Transactions per second: 50,000+
- Dashboard load time: < 3 seconds

### 4.2 Availability
- Uptime: 99.9%
- Disaster recovery: RPO < 1 hour
- Failover: Automatic

### 4.3 Security
- Authentication: Form-based with session management
- Authorization: Role-based access control (RBAC)
- Data encryption: TLS 1.2+ in transit, AES-256 at rest
- Audit logging: All user actions logged

### 4.4 Compliance
- PCI-DSS: PAN tokenization, access controls
- AML/CFT: Suspicious activity detection and reporting
- GDPR: Data retention policies, right to erasure

---

## 5. User Interface Overview

### 5.1 Dashboard
- Summary statistics cards
- Live alerts panel
- Transaction volume chart
- Risk breakdown chart
- Quick access navigation

### 5.2 Main Navigation
```
├── Dashboard
├── Transaction Monitoring
├── Case Management
│   ├── Active Cases
│   ├── Work Queues
│   └── Case Search
├── Alerts
├── Analytics
│   ├── Risk Heatmap
│   └── Reports
├── Compliance
│   ├── SAR Reports
│   └── Calendar
├── Merchant Directory
├── Settings
│   ├── Users
│   ├── Roles
│   └── Configuration
└── Audit Logs
```

---

## 6. Integration Specifications

### 6.1 Inbound APIs
| Endpoint | Method | Purpose |
|----------|--------|---------|
| /api/v1/transactions/ingest | POST | Transaction submission |
| /api/v1/clients/register | POST | Client registration |
| /api/v1/merchants | POST | Merchant onboarding |

### 6.2 Outbound Integrations
| System | Purpose |
|--------|---------|
| ML Scoring Service | Real-time fraud scoring |
| Sumsub | KYC/AML verification |
| Email/Slack | Notifications |

---

## 7. Glossary

| Term | Definition |
|------|------------|
| AML | Anti-Money Laundering |
| CFT | Countering the Financing of Terrorism |
| SAR | Suspicious Activity Report |
| IFTR | International Funds Transfer Report |
| KYC | Know Your Customer |
| EDD | Enhanced Due Diligence |
| PAN | Primary Account Number |
| MCC | Merchant Category Code |
| OFAC | Office of Foreign Assets Control |

---

## 8. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Jan 2026 | System | Initial release |
