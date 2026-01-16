# Software Requirements Specification
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026  
**Classification:** Internal

---

## 1. Introduction

### 1.1 Purpose
This SRS defines the software requirements for the AML Fraud Detector system, a comprehensive Anti-Money Laundering and Fraud Detection platform for payment gateway operations.

### 1.2 Scope
The system shall provide:
- Real-time transaction fraud detection
- AML/CFT compliance monitoring
- Sanctions screening
- Case management and workflow
- Regulatory reporting
- Administrative dashboard

### 1.3 Definitions and Acronyms

| Term | Definition |
|------|------------|
| AML | Anti-Money Laundering |
| CFT | Countering the Financing of Terrorism |
| SAR | Suspicious Activity Report |
| OFAC | Office of Foreign Assets Control |
| PAN | Primary Account Number |
| EMV | Europay, Mastercard, Visa (chip card standard) |
| MCC | Merchant Category Code |
| KYC | Know Your Customer |
| EDD | Enhanced Due Diligence |

---

## 2. Overall Description

### 2.1 Product Perspective
The AML Fraud Detector operates as a middleware system between merchant POS terminals and payment processors, providing real-time risk assessment before transaction authorization.

### 2.2 Product Functions
```
┌─────────────────────────────────────────────────────────────┐
│                    AML FRAUD DETECTOR                       │
├─────────────────────────────────────────────────────────────┤
│  F1. Transaction Ingestion & Real-time Processing           │
│  F2. ML-Based Fraud Scoring                                 │
│  F3. AML Risk Assessment                                    │
│  F4. Sanctions Screening                                    │
│  F5. Alert Generation & Management                          │
│  F6. Case Management & Workflow                             │
│  F7. Regulatory Reporting                                   │
│  F8. User & Access Management                               │
│  F9. Merchant Management                                    │
│  F10. Analytics & Dashboards                                │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 User Classes

| User Class | Description | Skill Level |
|------------|-------------|-------------|
| Compliance Officer | Reviews cases, files SARs | Expert |
| Fraud Analyst | Investigates fraud alerts | Intermediate |
| AML Analyst | Conducts AML screening | Intermediate |
| Administrator | Manages users and configuration | Expert |
| Viewer | Read-only dashboard access | Basic |

### 2.4 Operating Environment
- **Server OS**: Linux (Ubuntu 20.04+, RHEL 8+) or Windows Server 2019+
- **Java Runtime**: JDK 17 or higher
- **Database**: PostgreSQL 13+
- **Cache**: Redis 6+, Aerospike 6+
- **Browser**: Chrome 90+, Firefox 88+, Edge 90+

### 2.5 Design Constraints
- Must process transactions in under 200ms
- Must support 30,000+ concurrent connections
- Must comply with PCI-DSS requirements
- Must integrate with existing ML scoring infrastructure

---

## 3. Specific Requirements

### 3.1 External Interface Requirements

#### 3.1.1 User Interfaces
| ID | Requirement |
|----|-------------|
| UI-001 | System SHALL provide a responsive web dashboard |
| UI-002 | Dashboard SHALL support dark theme (Glassmorphism design) |
| UI-003 | Dashboard SHALL display real-time transaction metrics |
| UI-004 | Dashboard SHALL provide navigation to all system functions |
| UI-005 | System SHALL support minimum screen resolution 1280x720 |

#### 3.1.2 Hardware Interfaces
| ID | Requirement |
|----|-------------|
| HW-001 | System SHALL run on x64 architecture servers |
| HW-002 | Minimum RAM: 8GB (Recommended: 16GB+) |
| HW-003 | Minimum CPU: 4 cores (Recommended: 8+ cores) |

#### 3.1.3 Software Interfaces
| ID | Requirement |
|----|-------------|
| SI-001 | System SHALL integrate with PostgreSQL via JDBC |
| SI-002 | System SHALL integrate with Aerospike via native Java client |
| SI-003 | System SHALL integrate with Redis via Spring Data Redis |
| SI-004 | System SHALL integrate with ML Scoring Service via REST |
| SI-005 | System SHALL integrate with Sumsub via REST API |

#### 3.1.4 Communication Interfaces
| ID | Requirement |
|----|-------------|
| CI-001 | System SHALL expose REST APIs over HTTP/HTTPS |
| CI-002 | System SHALL use JSON as the data interchange format |
| CI-003 | System SHALL support TLS 1.2 or higher |
| CI-004 | System SHALL provide Swagger/OpenAPI documentation |

---

### 3.2 Functional Requirements

#### 3.2.1 Transaction Processing

| ID | Requirement | Priority |
|----|-------------|----------|
| TXN-001 | System SHALL accept transaction data via REST API | Critical |
| TXN-002 | System SHALL process transactions within 200ms | Critical |
| TXN-003 | System SHALL tokenize PAN using SHA-256 before storage | Critical |
| TXN-004 | System SHALL extract features for ML scoring | Critical |
| TXN-005 | System SHALL return fraud score and decision | Critical |
| TXN-006 | System SHALL support batch transaction processing | High |
| TXN-007 | System SHALL log all transactions for audit | Critical |

#### 3.2.2 Fraud Detection

| ID | Requirement | Priority |
|----|-------------|----------|
| FRD-001 | System SHALL integrate with external ML scoring service | Critical |
| FRD-002 | System SHALL apply configurable score thresholds | Critical |
| FRD-003 | System SHALL support BLOCK, HOLD, ALERT, ALLOW actions | Critical |
| FRD-004 | System SHALL maintain blocklists (PAN, terminal, merchant) | High |
| FRD-005 | System SHALL extract velocity features (1h, 24h, 7d, 30d) | High |
| FRD-006 | System SHALL extract EMV-specific features | Medium |
| FRD-007 | System SHALL calculate KYC Risk Score (KRS) for business and consumer users | High |
| FRD-008 | System SHALL calculate Transaction Risk Score (TRS) for each transaction | High |
| FRD-009 | System SHALL calculate Customer Risk Assessment (CRA) dynamically | High |
| FRD-010 | System SHALL track all scores (ML, KRS, TRS, CRA, Anomaly, Fraud, AML) | Critical |
| FRD-011 | System SHALL store all scores in database for audit and analysis | Critical |
| FRD-012 | System SHALL publish all scores in Kafka events | High |
| FRD-013 | System SHALL expose all scores in Prometheus metrics | High |
| FRD-014 | System SHALL display all scores in Grafana dashboards | High |

**Score Calculation Documentation:**
For detailed requirements and formulas for all scoring systems, see **[SCORING_PROCESS_DOCUMENTATION.md](SCORING_PROCESS_DOCUMENTATION.md)**.
| FRD-007 | System SHALL detect structuring patterns | High |

#### 3.2.3 AML Compliance

| ID | Requirement | Priority |
|----|-------------|----------|
| AML-001 | System SHALL perform sanctions screening | Critical |
| AML-002 | System SHALL support fuzzy name matching | High |
| AML-003 | System SHALL check against OFAC, UN, EU lists | Critical |
| AML-004 | System SHALL detect high-value transactions | High |
| AML-005 | System SHALL monitor velocity patterns | High |
| AML-006 | System SHALL identify high-risk countries | High |
| AML-007 | System SHALL calculate cumulative amounts (30d) | High |

#### 3.2.4 Alert Management

| ID | Requirement | Priority |
|----|-------------|----------|
| ALT-001 | System SHALL generate alerts from detection rules | Critical |
| ALT-002 | System SHALL support alert prioritization | High |
| ALT-003 | System SHALL allow alert assignment to analysts | High |
| ALT-004 | System SHALL track alert disposition | High |
| ALT-005 | System SHALL support bulk alert operations | Medium |
| ALT-006 | System SHALL provide alert aging reports | Medium |

#### 3.2.5 Case Management

##### 3.2.5.1 Case Creation
- **REQ-CM-001**: System SHALL create cases from rule violations, ML risk scores, sanctions hits, and graph anomalies
- **REQ-CM-002**: System SHALL track rule version and model version in case alerts for auditability
- **REQ-CM-003**: System SHALL automatically enrich cases with related transactions and merchant profiles
- **REQ-CM-004**: System SHALL link triggering transactions to cases asynchronously
- **REQ-CM-005**: System SHALL update graph context when cases are created

##### 3.2.5.2 Case Enrichment
- **REQ-CM-006**: System SHALL automatically link transactions to cases based on relationship type
- **REQ-CM-007**: System SHALL automatically link merchant profiles to cases
- **REQ-CM-008**: System SHALL trigger background KYC checks when merchants are linked to cases
- **REQ-CM-009**: System SHALL update Neo4j graph to flag merchants under investigation
- **REQ-CM-010**: System SHALL attach structured risk details as case notes

##### 3.2.5.3 Graph Anomaly Detection
- **REQ-CM-011**: System SHALL detect circular trading patterns (money loops) of length 3-6
- **REQ-CM-012**: System SHALL detect proximity to high-risk entities within 3 hops
- **REQ-CM-013**: System SHALL update merchant risk scores in graph based on case findings
- **REQ-CM-014**: System SHALL trigger cases automatically when graph anomalies are detected

##### 3.2.5.4 Case Archival & Retention
- **REQ-CM-015**: System SHALL track archival status and timestamp for compliance cases
- **REQ-CM-016**: System SHALL store cold storage reference (S3 path/ARN) for archived cases
- **REQ-CM-017**: System SHALL archive cases after 1 year, retain for 7 years total
- **REQ-CM-018**: System SHALL support querying archived cases with cold storage retrieval

##### 3.2.5.5 Case Management (Existing)

| ID | Requirement | Priority |
|----|-------------|----------|
| CSE-001 | System SHALL create cases from escalated alerts | Critical |
| CSE-002 | System SHALL support case workflow states | Critical |
| CSE-003 | System SHALL track case timeline | High |
| CSE-004 | System SHALL support document attachments | High |
| CSE-005 | System SHALL provide SLA tracking | Medium |
| CSE-006 | System SHALL support skill-based routing | Medium |
| CSE-007 | System SHALL generate network graphs | Low |

#### 3.2.6 Reporting

| ID | Requirement | Priority |
|----|-------------|----------|
| RPT-001 | System SHALL generate SAR reports | Critical |
| RPT-002 | System SHALL generate IFTR reports | High |
| RPT-003 | System SHALL support PDF export | High |
| RPT-004 | System SHALL support CSV export | High |
| RPT-005 | System SHALL track compliance deadlines | Medium |

#### 3.2.7 User Management

| ID | Requirement | Priority |
|----|-------------|----------|
| USR-001 | System SHALL support user authentication | Critical |
| USR-002 | System SHALL enforce role-based access control | Critical |
| USR-003 | System SHALL support password policies | High |
| USR-004 | System SHALL track concurrent sessions | High |
| USR-005 | System SHALL log user activities | Critical |

---

### 3.3 Non-Functional Requirements

#### 3.3.1 Performance

| ID | Requirement | Target |
|----|-------------|--------|
| PRF-001 | Transaction processing latency | < 200ms (P99) |
| PRF-002 | Concurrent connections supported | 30,000+ |
| PRF-003 | Transactions per second | 50,000+ |
| PRF-004 | Dashboard page load time | < 3 seconds |
| PRF-005 | Database query response time | < 100ms |

#### 3.3.2 Reliability

| ID | Requirement | Target |
|----|-------------|--------|
| REL-001 | System uptime | 99.9% |
| REL-002 | Mean time to recovery | < 15 minutes |
| REL-003 | Data durability | No data loss |
| REL-004 | Graceful degradation | Service continues if ML scoring fails |

#### 3.3.3 Scalability

| ID | Requirement | Target |
|----|-------------|--------|
| SCL-001 | Horizontal scaling | Support 2-10 application instances |
| SCL-002 | Database connections | Pool of 300 connections |
| SCL-003 | Request buffering capacity | 50,000 requests |

#### 3.3.4 Security

| ID | Requirement |
|----|-------------|
| SEC-001 | All data in transit SHALL be encrypted (TLS 1.2+) |
| SEC-002 | Passwords SHALL be hashed using BCrypt |
| SEC-003 | PAN data SHALL be tokenized before storage |
| SEC-004 | System SHALL enforce session timeout (configurable) |
| SEC-005 | System SHALL prevent CSRF attacks |
| SEC-006 | System SHALL sanitize input to prevent XSS |
| SEC-007 | System SHALL log all authentication attempts |

#### 3.3.5 Maintainability

| ID | Requirement |
|----|-------------|
| MNT-001 | System SHALL use external configuration (no hardcoding) |
| MNT-002 | System SHALL provide health check endpoints |
| MNT-003 | System SHALL log using structured format (SLF4J) |
| MNT-004 | System SHALL expose Prometheus metrics |
| MNT-005 | All thresholds SHALL be database-configurable |

#### 3.3.6 Auditability

| ID | Requirement |
|----|-------------|
| AUD-001 | System SHALL log all user actions |
| AUD-002 | System SHALL track configuration changes |
| AUD-003 | Audit logs SHALL be immutable |
| AUD-004 | Logs SHALL include timestamp, user, action, IP |

---

## 4. Data Requirements

### 4.1 Data Entities

| Entity | Description | Retention |
|--------|-------------|-----------|
| Transaction | Payment transaction records | 7 years |
| Alert | Generated alerts | 5 years |
| Case | Compliance cases | 7 years |
| User | System users | Indefinite |
| AuditLog | User activity logs | 7 years |
| Merchant | Merchant profiles | Indefinite |
| SARReport | Suspicious Activity Reports | 7 years |

### 4.2 Data Validation
- All monetary amounts in cents (integer)
- Timestamps in ISO 8601 format
- PANs validated using Luhn algorithm before hashing
- Email addresses validated with regex
- Phone numbers in E.164 format

---

## 5. Appendices

### 5.1 Requirement Traceability

| Business Need | Requirement IDs |
|---------------|-----------------|
| Fraud Prevention | TXN-*, FRD-* |
| AML Compliance | AML-*, CSE-*, RPT-001 |
| Operational Efficiency | ALT-*, CSE-*, USR-* |
| Regulatory Compliance | RPT-*, SEC-*, AUD-* |

### 5.2 Acceptance Criteria

Each requirement SHALL be considered complete when:
1. Implementation is code-complete
2. Unit tests pass with >80% coverage
3. Integration tests pass
4. Documentation is updated
5. Code review is approved

---

## 6. Document Approval

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Product Owner | | | |
| Technical Lead | | | |
| QA Lead | | | |
