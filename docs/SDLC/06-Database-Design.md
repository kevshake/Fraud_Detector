# Database Design Document
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026  
**Database:** PostgreSQL 13+

---

## 1. Overview

### 1.1 Database Architecture

The AML Fraud Detector uses a **multi-database architecture**:

| Database | Purpose | Data Types |
|----------|---------|------------|
| PostgreSQL | Primary RDBMS | Users, cases, alerts, audit logs, transactions (backup/audit) |
| Aerospike | High-speed primary storage | **Transactions (primary)**, sanctions lists, feature aggregates |
| Redis | Session/Stats cache | Statistics, rate limiting, sessions |

**Note:** Transactions are now stored primarily in Aerospike for fast access (< 1ms latency). PostgreSQL maintains a backup copy for compliance and audit purposes. See [AEROSPIKE_TRANSACTION_STORAGE.md](AEROSPIKE_TRANSACTION_STORAGE.md) for details.

### 1.2 Design Principles

1. **Normalization**: 3NF for transactional tables
2. **Indexing**: Comprehensive indexes for query performance
3. **Auditing**: All entities track created/updated timestamps
4. **Soft Deletes**: Where applicable for data retention
5. **JSON Storage**: JSONB for flexible schemas (EMV tags, etc.)

---

## 2. Entity Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              CORE DOMAIN                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐       ┌─────────────────┐       ┌─────────────────┐       │
│   │  merchants  │──1:N──│  transactions   │──1:1──│ transaction_    │       │
│   │             │       │                 │       │ features        │       │
│   └─────────────┘       └────────┬────────┘       └─────────────────┘       │
│         │                        │                                          │
│         │                        │1:N                                       │
│         │                        ▼                                          │
│         │               ┌─────────────────┐                                 │
│         │               │     alerts      │                                 │
│         │               └────────┬────────┘                                 │
│         │                        │N:1                                       │
│         │                        ▼                                          │
│         │               ┌─────────────────┐       ┌─────────────────┐       │
│         └──────N:1──────│compliance_cases │──1:N──│case_timeline    │       │
│                         └────────┬────────┘       └─────────────────┘       │
│                                  │1:N                                       │
│                                  ▼                                          │
│                         ┌─────────────────┐                                 │
│                         │   sar_reports   │                                 │
│                         └─────────────────┘                                 │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                           USER & ACCESS DOMAIN                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐       ┌─────────────────┐       ┌─────────────────┐       │
│   │    users    │──N:1──│     roles       │──N:M──│  permissions    │       │
│   └──────┬──────┘       └─────────────────┘       └─────────────────┘       │
│          │                                                                   │
│          │1:N                                                               │
│          ▼                                                                   │
│   ┌─────────────┐                                                           │
│   │ audit_logs  │                                                           │
│   └─────────────┘                                                           │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Table Specifications

### 3.1 Core Tables

#### transactions
```sql
CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    pan_hash        VARCHAR(64) NOT NULL,       -- SHA-256 tokenized PAN
    merchant_id     VARCHAR(50) NOT NULL,
    terminal_id     VARCHAR(50),
    amount_cents    BIGINT NOT NULL,
    currency        VARCHAR(3) DEFAULT 'KES',
    txn_timestamp   TIMESTAMP NOT NULL,
    emv_tags        JSONB,                      -- EMV chip data
    fraud_score     DECIMAL(5,4),               -- 0.0000 - 1.0000
    decision        VARCHAR(20),                -- BLOCK/HOLD/ALERT/ALLOW
    decision_reasons TEXT[],                    -- Array of reasons
    processing_time_ms INTEGER,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_merchant FOREIGN KEY (merchant_id) 
        REFERENCES merchants(merchant_id)
);

-- Indexes
CREATE INDEX idx_txn_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_txn_pan_hash ON transactions(pan_hash);
CREATE INDEX idx_txn_created_at ON transactions(created_at);
CREATE INDEX idx_txn_decision ON transactions(decision);
CREATE INDEX idx_txn_timestamp ON transactions(txn_timestamp);
```

#### transaction_features
```sql
CREATE TABLE transaction_features (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_id      BIGINT NOT NULL UNIQUE,
    -- Amount features
    amount_z_score      DECIMAL(10,4),
    -- Velocity features
    pan_txn_count_1h    INTEGER,
    pan_txn_count_24h   INTEGER,
    pan_amount_sum_24h  BIGINT,
    merchant_txn_count_1h INTEGER,
    -- Behavioral features
    distinct_terminals_30d INTEGER,
    avg_amount_30d      DECIMAL(15,2),
    -- EMV features
    is_chip_present     BOOLEAN,
    is_contactless      BOOLEAN,
    cvm_method          VARCHAR(20),
    -- Computed at
    computed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id)
);
```

#### alerts
```sql
CREATE TABLE alerts (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT,
    merchant_id     VARCHAR(50),
    alert_type      VARCHAR(50) NOT NULL,       -- HIGH_FRAUD_SCORE, SANCTIONS_MATCH, etc.
    priority        VARCHAR(20) DEFAULT 'MEDIUM', -- LOW/MEDIUM/HIGH/CRITICAL
    status          VARCHAR(30) DEFAULT 'OPEN',   -- OPEN/INVESTIGATING/RESOLVED
    description     TEXT,
    fraud_score     DECIMAL(5,4),
    risk_factors    JSONB,
    assigned_to     BIGINT,
    disposition     VARCHAR(30),                -- FALSE_POSITIVE/CONFIRMED_FRAUD/ESCALATED
    disposition_notes TEXT,
    resolved_at     TIMESTAMP,
    resolved_by     BIGINT,
    case_id         BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_alert_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id),
    CONSTRAINT fk_alert_assigned_to FOREIGN KEY (assigned_to) 
        REFERENCES users(id),
    CONSTRAINT fk_alert_case FOREIGN KEY (case_id) 
        REFERENCES compliance_cases(id)
);

CREATE INDEX idx_alert_status ON alerts(status);
CREATE INDEX idx_alert_type ON alerts(alert_type);
CREATE INDEX idx_alert_created_at ON alerts(created_at);
CREATE INDEX idx_alert_assigned_to ON alerts(assigned_to);
```

#### compliance_cases
```sql
CREATE TABLE compliance_cases (
    id                  BIGSERIAL PRIMARY KEY,
    case_number         VARCHAR(50) UNIQUE NOT NULL,
    case_type           VARCHAR(50) NOT NULL,    -- SANCTIONS_MATCH, FRAUD, AML, etc.
    status              VARCHAR(30) DEFAULT 'NEW',
    priority            VARCHAR(20) DEFAULT 'MEDIUM',
    merchant_id         VARCHAR(50),
    subject_name        VARCHAR(255),
    subject_type        VARCHAR(30),             -- INDIVIDUAL/ENTITY
    description         TEXT,
    risk_score          INTEGER,                 -- 0-100
    assigned_to         BIGINT,
    assigned_at         TIMESTAMP,
    sla_deadline        TIMESTAMP,
    resolution_summary  TEXT,
    resolved_at         TIMESTAMP,
    resolved_by         BIGINT,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archived            BOOLEAN DEFAULT FALSE,        -- Retention & Archival
    archived_at         TIMESTAMP,                     -- When case was archived
    archive_reference   VARCHAR(500),                  -- Cold storage reference (S3 path/ARN)
    psp_id              BIGINT                         -- Multi-tenancy support
    
    CONSTRAINT fk_case_assigned_to FOREIGN KEY (assigned_to) 
        REFERENCES users(id),
    CONSTRAINT fk_case_merchant FOREIGN KEY (merchant_id) 
        REFERENCES merchants(merchant_id)
);

CREATE INDEX idx_case_status ON compliance_cases(status);
CREATE INDEX idx_case_assigned_to ON compliance_cases(assigned_to);
CREATE INDEX idx_case_merchant_id ON compliance_cases(merchant_id);
CREATE INDEX idx_case_merchant ON compliance_cases(merchant_id);
CREATE INDEX idx_case_psp ON compliance_cases(psp_id);
CREATE INDEX idx_case_created ON compliance_cases(created_at);
```

### 3.2 Merchant Tables

#### merchants
```sql
CREATE TABLE merchants (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         VARCHAR(50) UNIQUE NOT NULL,
    business_name       VARCHAR(255) NOT NULL,
    legal_name          VARCHAR(255),
    registration_number VARCHAR(100),
    mcc_code            VARCHAR(4),
    risk_level          VARCHAR(20) DEFAULT 'MEDIUM',
    kyc_status          VARCHAR(30) DEFAULT 'PENDING',
    contract_status     VARCHAR(30) DEFAULT 'ACTIVE',
    daily_limit         BIGINT,                  -- In cents
    monthly_limit       BIGINT,
    contact_email       VARCHAR(255),
    contact_phone       VARCHAR(50),
    address_street      VARCHAR(255),
    address_city        VARCHAR(100),
    address_country     VARCHAR(2),              -- ISO 3166-1 alpha-2
    last_screened_at    TIMESTAMP,
    screening_status    VARCHAR(30),
    psp_id              BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_risk_level ON merchants(risk_level);
CREATE INDEX idx_merchant_kyc_status ON merchants(kyc_status);
CREATE INDEX idx_merchant_psp_id ON merchants(psp_id);
```

#### beneficial_owners
```sql
CREATE TABLE beneficial_owners (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         VARCHAR(50) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    ownership_percentage DECIMAL(5,2),
    nationality         VARCHAR(2),
    date_of_birth       DATE,
    id_document_type    VARCHAR(50),
    id_document_number  VARCHAR(100),
    screening_status    VARCHAR(30),
    last_screened_at    TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_bo_merchant FOREIGN KEY (merchant_id) 
        REFERENCES merchants(merchant_id)
);
```

### 3.3 User & Access Tables

#### users
```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    phone               VARCHAR(50),
    role_id             BIGINT NOT NULL,
    psp_id              BIGINT,                  -- For PSP-level isolation
    active              BOOLEAN DEFAULT TRUE,
    locked              BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    last_login          TIMESTAMP,
    password_changed_at TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_role_id ON users(role_id);
CREATE INDEX idx_user_psp_id ON users(psp_id);
```

#### roles
```sql
CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) UNIQUE NOT NULL,
    description     TEXT,
    is_system_role  BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default roles
INSERT INTO roles (name, description, is_system_role) VALUES
    ('SUPER_ADMIN', 'Full system access', TRUE),
    ('ADMIN', 'Administrative functions', TRUE),
    ('COMPLIANCE_OFFICER', 'Compliance management', TRUE),
    ('AML_ANALYST', 'AML investigation', TRUE),
    ('FRAUD_ANALYST', 'Fraud investigation', TRUE),
    ('VIEWER', 'Read-only access', TRUE);
```

#### role_permission_mappings
```sql
CREATE TABLE role_permission_mappings (
    id              BIGSERIAL PRIMARY KEY,
    role_id         BIGINT NOT NULL,
    permission      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_rpm_role FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE (role_id, permission)
);
```

#### audit_logs
```sql
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    user_email      VARCHAR(255),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(100),
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    result          VARCHAR(20),                -- SUCCESS/FAILURE
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
```

### 3.4 Compliance Tables

#### sar_reports
```sql
CREATE TABLE sar_reports (
    id                  BIGSERIAL PRIMARY KEY,
    sar_number          VARCHAR(50) UNIQUE NOT NULL,
    case_id             BIGINT,
    status              VARCHAR(30) DEFAULT 'DRAFT',
    subject_name        VARCHAR(255),
    subject_type        VARCHAR(30),
    suspicious_activity_type VARCHAR(100),
    narrative           TEXT,
    amount_involved     BIGINT,
    currency            VARCHAR(3),
    activity_start_date DATE,
    activity_end_date   DATE,
    filing_deadline     DATE,
    filed_at            TIMESTAMP,
    filed_by            BIGINT,
    fincen_reference    VARCHAR(100),
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sar_case FOREIGN KEY (case_id) 
        REFERENCES compliance_cases(id)
);
```

#### model_config
```sql
CREATE TABLE model_config (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(100) UNIQUE NOT NULL,
    value           VARCHAR(500) NOT NULL,
    description     TEXT,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(100)
);

-- Default configuration
INSERT INTO model_config (config_key, value, description) VALUES
    ('fraud.threshold.block', '0.95', 'Score threshold for blocking'),
    ('fraud.threshold.hold', '0.70', 'Score threshold for holding'),
    ('aml.high_value.threshold', '1000000', 'High-value threshold in cents'),
    ('velocity.pan.max_txns_1h', '10', 'Max PAN transactions per hour'),
    ('velocity.merchant.max_txns_1h', '50', 'Max merchant transactions per hour');
```

---

## 4. Aerospike Schema

### 4.1 Namespaces

| Namespace | Purpose | TTL |
|-----------|---------|-----|
| aml | General AML data | 30 days |
| sanctions | Sanctions lists | Permanent |
| cache | Feature cache | 24 hours |

### 4.2 Sets

#### sanctions.watchlist
```
Set: watchlist
Primary Key: name_hash (SHA-256)

Bins:
- name: String (original name)
- source: String (OFAC, UN, EU)
- type: String (INDIVIDUAL, ENTITY)
- country: String (ISO 3166)
- listing_date: Timestamp
- aliases: List<String>
- additional_info: Map
```

#### aml.statistics
```
Set: statistics
Primary Key: {type}:{id}:{window}

Bins:
- count: Long
- amount_sum: Long
- last_updated: Timestamp
```

---

## 5. Redis Schema

### 5.1 Key Patterns

| Pattern | Purpose | TTL |
|---------|---------|-----|
| `aml:stats:merchant:{id}:count:{window}` | Merchant txn count | 7 days |
| `aml:stats:merchant:{id}:amount:{window}` | Merchant amount sum | 7 days |
| `aml:stats:pan:{hash}:count:{window}` | PAN txn count | 7 days |
| `aml:stats:pan:{hash}:amount:{window}` | PAN amount sum | 7 days |
| `aml:stats:pan:{hash}:terminals:30d` | Distinct terminals (SET) | 30 days |
| `session:{sessionId}` | User session | 30 minutes |
| `rate_limit:{ip}` | Rate limiting | 1 minute |

---

## 6. Indexing Strategy

### 6.1 Index Summary

| Table | Index | Columns | Purpose |
|-------|-------|---------|---------|
| transactions | idx_txn_merchant_id | merchant_id | Merchant filter |
| transactions | idx_txn_pan_hash | pan_hash | PAN lookup |
| transactions | idx_txn_created_at | created_at | Date range |
| alerts | idx_alert_status | status | Status filter |
| compliance_cases | idx_case_status | status | Case queries |
| compliance_cases | idx_case_merchant | merchant_id | Merchant filter |
| compliance_cases | idx_case_psp | psp_id | PSP filter |
| compliance_cases | idx_case_created | created_at | Date range queries |
| audit_logs | idx_audit_created_at | created_at | Date queries |

### 6.2 Composite Indexes

```sql
-- Transaction search
CREATE INDEX idx_txn_search ON transactions(merchant_id, created_at DESC);

-- Alert queue
CREATE INDEX idx_alert_queue ON alerts(status, priority, created_at);

-- Case workload
CREATE INDEX idx_case_workload ON compliance_cases(assigned_to, status, sla_deadline);
```

---

## 7. Case Management Tables

### 7.1 case_alerts

```sql
CREATE TABLE case_alerts (
    id                  BIGSERIAL PRIMARY KEY,
    case_id             BIGINT NOT NULL,
    alert_type          VARCHAR(50) NOT NULL,        -- RULE_VIOLATION, ML_RISK, SANCTIONS_HIT, GRAPH_ANOMALY
    rule_name           VARCHAR(255),               -- Rule identifier
    rule_id             VARCHAR(100),                 -- Rule ID
    model_version       VARCHAR(100),                 -- ML model version (e.g., "XGBoost-v2.1")
    rule_version        VARCHAR(100),                 -- Rule version (e.g., "Policy-2023-Q4")
    score               DECIMAL(5,4),                -- Primary risk score (0.0-1.0) - ML score or rule override
    
    -- Comprehensive Score Tracking
    ml_score            DOUBLE PRECISION,             -- Machine Learning score (0.0-1.0)
    krs_score           DOUBLE PRECISION,             -- KYC Risk Score (0-100)
    trs_score           DOUBLE PRECISION,             -- Transaction Risk Score (0-100)
    cra_score           DOUBLE PRECISION,             -- Customer Risk Assessment (0-100)
    anomaly_score       DOUBLE PRECISION,             -- Anomaly detection score (0.0-1.0)
    fraud_score         DOUBLE PRECISION,             -- Fraud detection score (0-100+)
    aml_score           DOUBLE PRECISION,             -- AML risk score (0-100+)
    rule_score          DOUBLE PRECISION,              -- Rule-based score override (if applicable)
    risk_details_json   TEXT,                         -- JSON containing all risk details and component scores
    
    description         TEXT,
    raw_data            TEXT,                        -- JSON snapshot of triggering data
    triggered_at        TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_case_alert_case FOREIGN KEY (case_id) 
        REFERENCES compliance_cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_case_alert_case ON case_alerts(case_id);
CREATE INDEX idx_case_alert_type ON case_alerts(alert_type);
CREATE INDEX idx_case_alert_triggered ON case_alerts(triggered_at);
CREATE INDEX idx_case_alerts_ml_score ON case_alerts(ml_score);
CREATE INDEX idx_case_alerts_krs_score ON case_alerts(krs_score);
CREATE INDEX idx_case_alerts_trs_score ON case_alerts(trs_score);
CREATE INDEX idx_case_alerts_cra_score ON case_alerts(cra_score);
```

**Score Fields Description:**

| Field | Type | Range | Description |
|-------|------|-------|-------------|
| `score` | DECIMAL(5,4) | 0.0-1.0 | Primary risk score (ML score or rule override) |
| `ml_score` | DOUBLE PRECISION | 0.0-1.0 | Machine Learning score from XGBoost model |
| `krs_score` | DOUBLE PRECISION | 0-100 | KYC Risk Score - customer/merchant profile risk |
| `trs_score` | DOUBLE PRECISION | 0-100 | Transaction Risk Score - transaction-specific risk |
| `cra_score` | DOUBLE PRECISION | 0-100 | Customer Risk Assessment - evolving customer risk |
| `anomaly_score` | DOUBLE PRECISION | 0.0-1.0 | Anomaly detection score (reconstruction error) |
| `fraud_score` | DOUBLE PRECISION | 0-100+ | Fraud detection score (rule-based points) |
| `aml_score` | DOUBLE PRECISION | 0-100+ | AML risk score (rule-based points) |
| `rule_score` | DOUBLE PRECISION | 0.0-1.0 | Rule-based score override (if applicable) |
| `risk_details_json` | TEXT | JSON | Complete risk context and component scores |

**Migration:** See `V102__add_score_fields_to_case_alerts.sql` for the migration script.

**Score Calculation Documentation:** For detailed formulas and calculation methods, see **[SCORING_PROCESS_DOCUMENTATION.md](../SCORING_PROCESS_DOCUMENTATION.md)**.

### 7.2 case_transactions

Links transactions to cases for enrichment and investigation.

```sql
CREATE TABLE case_transactions (
    id                  BIGSERIAL PRIMARY KEY,
    case_id             BIGINT NOT NULL,
    transaction_id      BIGINT NOT NULL,
    relationship_type   VARCHAR(50) NOT NULL,        -- TRIGGERING_TRANSACTION, RELATED_TRANSACTION
    added_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by            BIGINT,                     -- User who added (null for system)
    
    CONSTRAINT fk_ct_case FOREIGN KEY (case_id) 
        REFERENCES compliance_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_transaction FOREIGN KEY (transaction_id) 
        REFERENCES transactions(id),
    UNIQUE (case_id, transaction_id)
);

CREATE INDEX idx_case_transaction_case ON case_transactions(case_id);
CREATE INDEX idx_case_transaction_txn ON case_transactions(transaction_id);
```

### 7.3 case_entities

Links merchants, customers, or other entities to cases.

```sql
CREATE TABLE case_entities (
    id                  BIGSERIAL PRIMARY KEY,
    case_id             BIGINT NOT NULL,
    entity_type         VARCHAR(50) NOT NULL,        -- MERCHANT, CUSTOMER, BENEFICIAL_OWNER
    entity_reference    VARCHAR(255) NOT NULL,       -- Merchant ID, customer ID, etc.
    description         TEXT,
    added_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ce_case FOREIGN KEY (case_id) 
        REFERENCES compliance_cases(id) ON DELETE CASCADE
);

CREATE INDEX idx_case_entity_case ON case_entities(case_id);
CREATE INDEX idx_case_entity_type_ref ON case_entities(entity_type, entity_reference);
```

---

## 8. Data Retention & Archival

### 8.1 Retention Policy

| Data Type | Retention Period | Archival |
|-----------|------------------|----------|
| Transactions | 7 years | Archive after 1 year |
| Alerts | 5 years | Archive after 6 months |
| Cases | 7 years | Archive after 1 year |
| Audit Logs | 7 years | Archive after 1 year |
| SAR Reports | 7 years | Never delete |
| Session Data | 30 minutes | Auto-expire |
| Statistics | 30 days | Auto-expire |

### 8.2 Case Archival Fields

The `compliance_cases` table includes archival tracking:

- **archived** (BOOLEAN): Indicates if case has been archived
- **archived_at** (TIMESTAMP): When case was archived
- **archive_reference** (VARCHAR): Reference to cold storage (S3 path/ARN)

**Archival Process:**
1. Cases older than 1 year are marked for archival
2. Case data exported to cold storage (S3, Glacier, etc.)
3. `archived` flag set to `true`
4. `archive_reference` stores cold storage location
5. Case remains queryable but data retrieved from cold storage when needed

---

## 8. Migration Scripts

### 8.1 Initial Schema

```sql
-- V1__Initial_Schema.sql
-- Creates all base tables
-- Located: src/main/resources/db/migration/V1__Initial_Schema.sql
```

### 8.2 Migration Naming Convention

```
V{version}__{description}.sql

Examples:
V1__Initial_Schema.sql
V2__Add_PSP_Tables.sql
V3__Add_Case_Timeline.sql
```

---

## 9. Backup Strategy

### 9.1 Backup Schedule

| Type | Frequency | Retention |
|------|-----------|-----------|
| Full Backup | Daily 2 AM | 30 days |
| Incremental | Hourly | 7 days |
| Transaction Log | Continuous | 7 days |

### 9.2 Recovery Objectives

- **RPO**: < 1 hour
- **RTO**: < 15 minutes

---

## Appendix: Quick Reference

### A. Common Queries

```sql
-- Daily transaction volume
SELECT DATE(created_at), COUNT(*), SUM(amount_cents)
FROM transactions
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at);

-- Open cases by analyst
SELECT u.email, COUNT(*) as open_cases
FROM compliance_cases c
JOIN users u ON c.assigned_to = u.id
WHERE c.status NOT IN ('RESOLVED', 'CLOSED')
GROUP BY u.email;

-- Alert disposition rate
SELECT disposition, COUNT(*) * 100.0 / SUM(COUNT(*)) OVER () as percentage
FROM alerts
WHERE resolved_at > NOW() - INTERVAL '30 days'
GROUP BY disposition;
```

### B. Performance Tuning

```sql
-- Optimize for high-volume inserts
ALTER TABLE transactions SET (autovacuum_vacuum_scale_factor = 0.01);

-- Parallel query for reports
SET max_parallel_workers_per_gather = 4;
```
