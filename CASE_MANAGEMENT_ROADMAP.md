# AML Case Management System - Implementation Roadmap

> **Regulatory Compliance Case Management Platform**
> Turning alerts into investigations, enforcing regulated workflows, capturing human decisions, and producing audit-proof evidence.

---

## Executive Summary

This roadmap defines the implementation of a **regulator-compliant Case Management System** that integrates with our existing ML/Graph AML detection infrastructure. The system ensures:

- **Explainability**: Every decision is documented and reproducible
- **Auditability**: Immutable audit trails for all actions
- **Regulatory Alignment**: CBK, FATF, and FinCEN requirements
- **Human-in-the-Loop**: Controlled investigator workflows with RBAC

---

## Pre-requisites (From Existing System)

- [x] XGBoost ML scoring (`ScoringService.java`)
- [x] Neo4j Graph Analytics (`Neo4jGdsService.java`)
- [x] Drools Rules Engine (`DroolsRulesService.java`)
- [x] Aerospike Caching (`AerospikeGraphCacheService.java`)
- [x] Regulatory Compliance (`RegulatoryComplianceService.java`)
- [x] XGBoost Explainability (`XGBoostExplainabilityService.java`)
- [x] Prometheus Metrics (`GraphMetricsExporter.java`)

---

## Phase 1: Case Data Model & Core Entities (Week 1-2)

### 1.1 Database Schema Design
- [x] Create `aml_case` master table
- [x] Create `case_entity` (linked customers/accounts/transactions)
- [x] Create `case_alert` (triggering alerts)
- [x] Create `case_action` (investigator actions)
- [x] Create `case_evidence` (documents, screenshots)
- [x] Create `case_decision` (final outcome)
- [x] Create `case_audit_log` (immutable trail - INSERT ONLY)
- [x] Add foreign key constraints and indexes

### 1.2 JPA Entities
- [x] Create `AmlCase.java` entity (Mapped as `ComplianceCase`)
- [x] Create `CaseEntity.java` entity
- [x] Create `CaseAlert.java` entity
- [x] Create `CaseAction.java` entity
- [x] Create `CaseEvidence.java` entity
- [x] Create `CaseDecision.java` entity
- [x] Create `CaseAuditLog.java` entity (immutable)

### 1.3 Case States & Lifecycle
- [x] Define `CaseStatus` enum:
  - `OPEN`, `UNDER_REVIEW`, `PENDING_INFO`, `ESCALATED`
  - `CLOSED_APPROVED`, `CLOSED_REJECTED`, `SAR_FILED`
- [x] Implement state machine for transitions
- [x] Validate transition rules (e.g., cannot skip UNDER_REVIEW)
- [x] Log all transitions with timestamp and user

---

## Phase 2: Case Creation & Triggering Logic (Week 3-4)

### 2.1 Alert-to-Case Conversion
- [x] Create `CaseCreationService.java`
- [x] Implement automatic case creation triggers:
  - [x] Rule violation (from Drools)
  - [x] Sanctions hit (from Aerospike screening)
  - [x] Threshold breach (CBK CTR/STR)
  - [x] Jurisdiction block (FATF/CBK)
  - [x] ML high-risk score (XGBoost > threshold)
  - [x] Graph anomaly (cycle detection, mule proximity)
- [x] Implement case deduplication (same entity within window)
- [x] Implement case priority scoring

### 2.2 Integration with Detection Systems
- [x] Connect to `DroolsRulesService` for rule-triggered cases
- [x] Connect to `RegulatoryComplianceService` for threshold cases
- [x] Connect to `Neo4jGdsService` for graph-triggered cases
- [x] Connect to `ScoringService` for ML-triggered cases
- [x] Implement async case creation (non-blocking)

### 2.3 Case Enrichment
- [x] Auto-attach triggering transaction(s)
- [x] Auto-attach related transactions (24h window)
- [x] Auto-attach customer KYC data (`CaseEnrichmentService` stores generic entities)
- [x] Auto-attach ML explanation (SHAP/feature importance)
- [x] Auto-attach graph context (community, connections)

---

## Phase 3: Investigator Workflow & UI (Week 5-6)

### 3.1 Case Queue & Assignment
- [x] Create `CaseQueueService.java`
- [x] Implement work queue by priority
- [x] Implement case assignment (manual + auto)
- [x] Implement load balancing across investigators
- [x] Track SLA timers per case

### 3.2 Investigator Actions
- [x] `AddNoteAction` - Add investigation notes
- [x] `RequestInfoAction` - Request additional documents
- [x] `AttachEvidenceAction` - Upload files/screenshots
- [x] `EscalateAction` - Escalate to senior analyst
- [x] `MakeDecisionAction` - Record final determination
- [x] Each action creates immutable audit record

### 3.3 Case Review UI (API Endpoints)
- [x] `GET /compliance/cases` - List cases (with filters)
- [x] `GET /compliance/cases/{id}` - Get case details
- [x] `GET /api/cases/{id}/timeline` - Get case timeline
- [x] `GET /api/cases/{id}/evidence` - Get evidence list (via `EvidenceController`)
- [ ] `GET /api/cases/{id}/transactions` - Get related transactions
- [x] `GET /api/cases/{id}/graph` - Get graph visualization data
- [x] `POST /api/cases/{id}/actions` - Record action (Escalate, Assign)
- [x] `POST /api/cases/{id}/decisions` - Record decision

---

## Phase 4: Evidence Handling (Week 7-8)

### 4.1 Object Storage Integration
- [x] Configure S3-compatible object storage
- [x] Create `EvidenceStorageService.java`
- [x] Implement file upload with size/type validation
- [x] Generate SHA-256 hash for each file
- [x] Store hash + metadata in database
- [x] Implement secure pre-signed URLs for download (or local path access)
(Note: Using local filesystem for dev environment)

### 4.2 Evidence Types
- [x] Identity documents (passport, ID)
- [x] Transaction screenshots
- [x] Bank statements
- [x] External database results
- [x] Analyst notes/memos
- [x] Communication records

### 4.3 Evidence Integrity
- [x] Implement hash verification on download
- [x] Prevent file modification after upload
- [x] Record uploader, timestamp, file type
- [x] Support court admissibility requirements

---

## Phase 5: Decisioning & Regulatory Reporting (Week 9-10)

### 5.1 Decision Framework
- [x] Create `CaseDecisionService.java`
- [x] Implement decision types:
  - `APPROVE` - Transaction/entity cleared
  - `REJECT` - Transaction declined
  - `FREEZE_ACCOUNT` - Account frozen pending review
  - `EDD_REQUIRED` - Enhanced Due Diligence
  - `FILE_SAR` - Suspicious Activity Report
  - `FILE_STR` - Suspicious Transaction Report
- [x] Enforce mandatory decision justification
- [x] Require evidence attachment for SAR/STR

### 5.2 SAR/STR Generation
- [x] Create `SarReportService.java`
- [x] Implement CBK STR format (Kenya)
- [ ] Implement FinCEN SAR format (if needed)
- [x] Auto-populate from case data
- [x] Generate structured XML/JSON report
- [x] Track submission status

### 5.3 Regulatory Submission
- [ ] Implement submission channel integration
- [ ] Store submission reference/confirmation
- [ ] Track regulatory response
- [ ] Generate compliance reports

---

## Phase 6: Audit Trail & Compliance (Week 11-12)

### 6.1 Immutable Audit Log
- [x] Create `CaseAuditService.java`
- [x] Implement INSERT-ONLY audit table
- [x] Disable UPDATE/DELETE on audit tables
- [x] Record: timestamp, user, action, before/after state
- [ ] Hash chain for tamper detection

### 6.2 Case Replay Capability
- [x] Implement full case history reconstruction
- [x] Show who did what and when
- [x] Reproduce ML scores at decision time (via `CaseAlert.modelVersion`)
- [x] Show policy/rule version used (via `CaseAlert.ruleVersion`)
- [x] Export case for regulatory review (via `CaseAuditService.exportCaseReport()`)

### 6.3 Retention & Archival
- [x] Implement tiered storage (hot/warm/cold)
- [x] Enforce regulatory retention periods:
  - CBK: 7 years
  - FATF: 5 years minimum
- [x] WORM-style storage for archived cases

---

## Phase 7: Role-Based Access Control (Week 13)

### 7.1 Roles & Permissions
| Role | Permissions |
|------|-------------|
| `ANALYST` | Review cases, add notes, request info |
| `SENIOR_ANALYST` | Decide, escalate, close cases |
| `COMPLIANCE_OFFICER` | Approve SAR/STR, override decisions |
| `AUDITOR` | Read-only access to all cases |
| `ADMIN` | Policy config, user management, no case access |

### 7.2 Implementation
- [x] Create `CasePermissionService.java`
- [x] Implement role-based method security
- [x] Enforce segregation of duties
- [x] Audit permission checks
- [x] Prevent privilege escalation

---

## Phase 8: Kafka Event Streaming & Integration (Week 14)

### 8.1 Kafka Architecture
- [x] Add `spring-kafka` dependency
- [x] Configure topics:
  - `aml.case.lifecycle` (Created, Status Changed)
  - `aml.case.decision` (Approved, Rejected, SAR Filed)
  - `aml.compliance.alert` (Triggering events)
- [x] Implement Producers: `CaseEventProducer`
- [x] Implement Consumers: `NotificationConsumer`, `ReportingConsumer`

### 8.2 Event-Driven Workflows
- [x] Async Case Creation: Listen to `aml.risk.score` topic from XGBoost
- [x] Async Reporting: Listen to `aml.case.decision` for SAR generation
- [x] Notifications: Listen to `aml.case.lifecycle` for email/SMS
- [x] **Email Provider**: Integrate Brevo API (Free Tier) for transactional emails (Roadmap created)
- [ ] Real-time Dashboard: WebSocket push from Kafka consumer

### 8.3 Notification Content Policy (STRICT)
> **Legal Requirement**: Never leak AML logic or suspicions to customers.

| Allowed Emails | Forbidden Emails |
|---|---|
| "Documents required" | Exact AML rule triggered |
| "Case closed" | Risk scores |
| "Account under review" (Generic) | "Suspicious activity detected" |
| "Transaction declined" (Generic) | SAR-related details |

### 8.4 System Integrations
- [x] Transaction engine (real-time alerts via Kafka)
- [x] KYC services (customer data enrichment)
- [x] Neo4j (graph context updates)
- [x] Reporting systems (dashboards)

---

## Phase 9: Performance & Scalability (Week 15)

### 9.1 Performance Optimization
- [x] Async case creation (non-blocking)
- [x] Cache case summaries in Aerospike
- [x] Index case metadata for search
- [x] Implement pagination for large lists
- [x] Background jobs for enrichment

### 9.2 Scalability
- [x] Stateless case service design
- [x] Horizontal scaling support
- [x] Database connection pooling
- [x] Object storage redundancy

---

## Regulatory Compliance Checklist

| Requirement | Implementation | Status |
|-------------|----------------|--------|
| Replay full case history | `CaseAuditService.replayCase()` | [x] |
| Show who did what and when | Immutable audit log | [x] |
| Justify all decisions | Mandatory decision notes | [x] |
| Reproduce ML scores | Score snapshots stored | [x] |
| Prove policy version used | Rule version tracking | [x] |
| SAR/STR filing | `SarReportService` | [x] |
| Retention compliance | 7-year retention policy | [ ] |
| Evidence integrity | SHA-256 + WORM storage | [x] |

---

## Technology Stack

| Layer | Technology |
|-------|------------|
| Case Service | Java Spring Boot |
| Database | PostgreSQL (primary) + Aerospike (cache) |
| Object Storage | S3-compatible (MinIO) |
| Search | OpenSearch / Elasticsearch |
| Workflow | Spring State Machine |
| Graph Context | Neo4j |
| ML Explainability | XGBoost SHAP values |
| Security | Spring Security + mTLS + RBAC |
| Events | Spring Events (local) / Kafka (distributed) |

---

## Key Success Metrics

| Metric | Target |
|--------|--------|
| Case creation latency | < 100ms |
| Case retrieval latency | < 50ms |
| SAR/STR generation time | < 5 seconds |
| Audit log query time | < 1 second |
| Evidence upload time | < 3 seconds |
| Case SLA compliance | > 95% |

---

## Risk & Mitigation

| Risk | Mitigation |
|------|------------|
| Audit trail tampering | Append-only tables, hash chains |
| Evidence modification | SHA-256 hashing, WORM storage |
| Privilege escalation | RBAC with audit, segregation of duties |
| Data loss | Multi-region replication, backups |
| Regulatory non-compliance | Regular compliance audits, training |

---

## Notes

- Mark items with `[x]` when completed
- Each phase should have unit tests and integration tests
- Document all API endpoints with OpenAPI/Swagger
- Conduct security review before each phase completion
- Update this roadmap as requirements evolve
