# AML Merchant Screening Application - Progress Tracker

## Vision

Transform the current AML Fraud Detector into a comprehensive **Anti-Money Laundering Merchant Screening Application** that:
- ✅ Receives merchant data during onboarding and updates
- ✅ Screens merchants against sanctions, PEP, and adverse media lists
- ✅ Stores screening results with risk scores
- ✅ Raises cases for manual compliance review
- ✅ Integrates with external AML service providers
- ✅ Provides ongoing monitoring and alerts

---

## Core Requirements

### 1. Merchant Data Points to Collect

#### Basic Merchant Information
- ✅ **Legal name** - Official registered business name
- ✅ **Trading name / DBA** - "Doing Business As" name
- ✅ **Country** - Country of incorporation
- ✅ **Address** - Registered business address
- ✅ **Registration number** - Business registration/license number
- ✅ **National ID / Tax ID** - VAT number, EIN, etc.

#### Business Details
- ✅ **Industry** - MCC (Merchant Category Code) or business category
- ✅ **Expected transaction volume** - Monthly/yearly volume
- ✅ **Transaction channel** - Online, in-store, mobile, etc.
- ✅ **Business type** - Corporation, LLC, Partnership, Sole Proprietor
- ✅ **Website / Social media** - Online presence

#### Ownership Information (UBOs - Ultimate Beneficial Owners)
For each owner with ≥25% ownership:
- ✅ **Full name** - Legal name
- ✅ **Date of birth (DOB)** - For age verification and PEP screening
- ✅ **Nationality** - Country of citizenship
- ✅ **Country of residence** - Current residence
- ✅ **Identification documents** - Passport number, national ID
- ✅ **Ownership percentage** - % of business owned

---

### 2. What to Screen Against

#### Sanctions Lists (Must-Have)
- ✅ **OFAC** (Office of Foreign Assets Control - US)
- ✅ **UN Security Council** sanctions lists
- ✅ **EU Consolidated List** of sanctions
- ✅ **Local/National sanctions lists** (per operating country)
- ✅ **HM Treasury UK** sanctions list
- ✅ **Other jurisdictional lists** (DFAT Australia, etc.)

#### PEP Lists (Politically Exposed Persons)
- ✅ **Current PEPs** - Active government officials
- ✅ **Former PEPs** - Retired officials (time-based)
- ✅ **PEP Relatives & Close Associates (RCAs)**
- ✅ **PEP sub-classification** by influence level

#### Adverse Media
- ✅ **Financial crime** - Money laundering, fraud, corruption
- ✅ **Terrorism** - Terrorist financing, terrorism links
- ✅ **Organized crime** - Criminal organizations
- ✅ **Regulatory violations** - Compliance breaches
- ✅ **Negative news** - Scandals, investigations

#### Internal Lists
- ✅ **Internal blacklist** - Previously rejected/blocked merchants
- ✅ **Internal greylist** - Merchants under enhanced monitoring
- ✅ **High-risk industry list** - Industries requiring EDD
- ✅ **High-risk geography list** - Countries requiring enhanced checks

---

### 3. When to Screen (Screening Triggers)

#### Onboarding Screening (Before Approval)
- ✅ **Mandatory for all new merchants**
- ✅ Screen merchant entity (business name, registration number)
- ✅ Screen all beneficial owners (UBOs)
- ✅ Screen against ALL lists (sanctions, PEP, adverse media, internal)
- ✅ Generate risk score
- ✅ Create case if matches found or high risk
- ✅ **Decision:** APPROVE, REVIEW, REJECT, EDD_REQUIRED

#### Update Screening (On Significant Changes)
Trigger re-screening when:
- ✅ **Name change** - Legal name or trading name modification
- ✅ **Ownership change** - New owner added, ownership % change >10%
- ✅ **Address change** - Particularly cross-border relocation
- ✅ **Business type change** - Change in MCC or industry
- ✅ **Volume increase** - Significant transaction volume increase (>50%)
- ✅ **New UBO** - Addition of beneficial owner

#### Periodic Rescreening (Ongoing Monitoring)
- ✅ **Frequency:** Daily or Weekly (configurable)
- ✅ Screen against **updated sanctions lists** (lists change frequently)
- ✅ Screen against **updated PEP lists**
- ✅ Monitor for **new adverse media**
- ✅ Auto-generate alerts on new matches
- ✅ Create cases for manual review
- ✅ Update risk scores

#### Event-Based Screening
- ✅ **Transaction anomaly** - Unusual transaction patterns detected
- ✅ **Geographic risk change** - Operating in newly sanctioned country
- ✅ **Regulatory update** - New compliance requirements
- ✅ **Customer complaint** - Reports of suspicious activity
- ✅ **Manual trigger** - Compliance officer requests re-screen

---

### Impact on System Design

**Data Model:** Drives creation of:
- `merchants` table (all merchant data points)
- `beneficial_owners` table (UBO details)
- `merchant_screening_results` table (screening outcomes)
- `owner_screening_results` table (UBO screening outcomes)

**API Design:** Requires endpoints for:
- Merchant onboarding with data collection
- Update merchant with re-screening triggers
- Manual screening triggers
- Periodic batch screening

**Workflow:** Defines:
- Onboarding workflow with screening gates
- Change detection and re-screening logic
- Scheduled periodic rescreening jobs
- Alert generation and case creation rules

---

## Current Status

### ✅ Completed (Existing Features)

#### Core Fraud Detection
- [x] Transaction ingestion and processing
- [x] Feature extraction (30+ features)
- [x] ML-based fraud scoring (XGBoost integration)
- [x] Decision engine (BLOCK/HOLD/ALLOW)
- [x] Alert generation and case management
- [x] Feedback and labeling system
- [x] Model performance monitoring

#### Infrastructure
- [x] Spring Boot 3.2.0 application
- [x] PostgreSQL database (6 tables)
- [x] REST Assured for API calls
- [x] HikariCP connection pooling (300 connections)
- [x] Async processing (CompletableFuture)
- [x] High throughput optimization (30K+ concurrent requests)
- [x] HTTP/2 support with auto-failover
- [x] Aerospike connection setup (not active)

#### Database Schema
- [x] `model_config` - Configurable thresholds
- [x] `transactions` - Transaction data
- [x] `transaction_features` - Extracted features
- [x] `alerts` - Generated alerts
- [x] `model_metrics` - Performance metrics
- [x] `clients` - Client registration

---

## 🚧 In Progress / Planned Features

### Phase 1: Merchant Onboarding & Data Management

#### 1.1 Merchant Data Model
- [x] Create `merchants` table
  - [x] Merchant ID (primary key)
  - [x] Business name / DBA
  - [x] Legal entity name
  - [x] Business registration number
  - [x] Tax ID / VAT number
  - [x] Business type / category
  - [x] MCC (Merchant Category Code)
  - [x] Country of incorporation
  - [x] Operating countries
  - [x] Business address
  - [x] Contact information
  - [x] Website / social media
  - [x] Ownership structure
  - [x] Created/updated timestamps
  - [x] Status (ACTIVE, SUSPENDED, UNDER_REVIEW, BLOCKED)

#### 1.2 Beneficial Owners (UBO) Data Model
- [x] Create `beneficial_owners` table
  - [x] Owner ID (primary key)
  - [x] Merchant ID (foreign key)
  - [x] Full name
  - [x] Date of birth
  - [x] Nationality
  - [x] Country of residence
  - [x] Identification documents (passport, national ID)
  - [x] Ownership percentage
  - [x] PEP status
  - [x] Sanctions status
  - [x] Created/updated timestamps

#### 1.3 Merchant Onboarding Service
- [x] Create `MerchantOnboardingService`
  - [x] Receive merchant application data
  - [x] Validate merchant data
  - [x] Store merchant information
  - [x] Trigger initial AML screening
  - [x] Generate onboarding case

#### 1.4 Merchant Update Service
- [x] Create `MerchantUpdateService`
  - [x] Handle merchant data changes
  - [x] Track change history (via AuditTrail)
  - [x] Trigger re-screening on significant changes
  - [x] Update risk scores (implicitly via Orchestrator re-screen)

---

### Phase 2: AML Screening Integration

#### 2.1 Sanctions Screening Approach

**Option A: Roll-Your-Own (Recommended)** ⭐
- [x] Implement in-house sanctions screening (see `ROLL_YOUR_OWN_SANCTIONS_SCREENING.md`)
- [x] Download OFAC/UN/EU lists daily from OpenSanctions
- [x] Store sanctions data in PostgreSQL
- [x] Implement hybrid name-matching (Double Metaphone + Levenshtein)
- [ ] Benefits:
  - ✅ Free (no API costs)
  - ✅ Unlimited screening
  - ✅ Full control
  - ✅ Faster (no external API latency)
  - ✅ Works offline

**Option B: External AML Service Provider**
- [x] Select provider (ComplyAdvantage, Sumsub, etc. - see `AML_SERVICE_PROVIDERS_RESEARCH.md`)
- [x] Integrate via REST API
- [ ] Benefits:
  - ✅ Includes adverse media screening
  - ✅ Comprehensive PEP coverage
  - ✅ Less maintenance
  - ❌ Costs $100-$1,000+/month
  - ❌ API rate limits

**Option C: Hybrid Approach** (Best of Both)
- [x] Roll-your-own for sanctions screening (free)
- [x] External API for adverse media only
- [x] External API for enhanced PEP coverage (if needed)

#### 2.2 AML Screening Configuration
- [x] Add AML provider configuration to `application.properties`
  ```properties
  aml.provider.enabled=true
  aml.provider.name=complyadvantage
  aml.provider.api.url=https://api.complyadvantage.com
  aml.provider.api.key=${AML_PROVIDER_API_KEY}
  aml.provider.timeout.ms=5000
  aml.provider.retry.max=3
  ```

#### 2.3 AML Screening Data Model
- [x] Create `merchant_screening_results` table
  - [x] Screening ID (primary key)
  - [x] Merchant ID (foreign key)
  - [x] Screening type (SANCTIONS, PEP, ADVERSE_MEDIA, WATCHLIST)
  - [x] Screening provider
  - [x] Screening status (CLEAR, MATCH, POTENTIAL_MATCH)
  - [x] Match score
  - [x] Match details (JSON)
  - [x] Screened at timestamp
  - [x] Screened by (user/system)
  - [x] Notes

#### 2.4 Beneficial Owner Screening
- [x] Create `owner_screening_results` table
  - [x] Screening ID (primary key)
  - [x] Owner ID (foreign key)
  - [x] Merchant ID (foreign key)
  - [x] Screening type
  - [x] Screening status
  - [x] Match score
  - [x] Match details (JSON)
  - [x] Screened at timestamp

---

### Phase 3: Risk Scoring & Decision Engine

#### 3.1 Merchant Risk Scoring Service
- [x] Create `MerchantRiskScoringService` (`RiskAssessmentService`)
  - [x] Calculate composite risk score
  - [x] Factors:
    - [x] AML screening results (sanctions, PEP, adverse media)
    - [x] Transaction patterns
    - [x] Geographic risk (high-risk countries)
    - [x] Business type risk (MCC-based)
    - [x] Ownership structure risk
    - [x] Historical compliance issues
  - [x] Risk categories: LOW, MEDIUM, HIGH, CRITICAL

#### 3.2 Risk Score Data Model
- [x] Create `merchant_risk_scores` table
  - [x] Score ID (primary key)
  - [x] Merchant ID (foreign key)
  - [x] Overall risk score (0-100)
  - [x] Risk level (LOW, MEDIUM, HIGH, CRITICAL)
  - [x] Component scores (JSON):
    - [x] AML screening score
    - [x] Transaction risk score
    - [x] Geographic risk score
    - [x] Business risk score
    - [x] Ownership risk score
  - [x] Calculated at timestamp
  - [x] Valid until timestamp
  - [x] Notes

#### 3.3 Enhanced Decision Engine
- [x] Extend `DecisionEngine` for merchants
  - [x] Merchant risk-based decisions
  - [x] Configurable risk thresholds
  - [x] Actions:
    - [x] APPROVE: Low risk, proceed with onboarding
    - [x] REVIEW: Medium risk, manual review required
    - [x] REJECT: High risk, deny onboarding
    - [x] ENHANCED_DUE_DILIGENCE: Additional documentation needed

---

### Phase 4: Case Management & Manual Review

#### 4.1 Case Management Data Model
- [x] Create `compliance_cases` table
  - [x] Case ID (primary key)
  - [x] Merchant ID (foreign key)
  - [x] Case type (ONBOARDING, PERIODIC_REVIEW, ALERT, UPDATE)
  - [x] Case status (OPEN, IN_PROGRESS, RESOLVED, ESCALATED, CLOSED)
  - [x] Priority (LOW, MEDIUM, HIGH, URGENT)
  - [x] Assigned to (compliance officer)
  - [x] Created at
  - [x] Due date
  - [x] Resolved at
  - [x] Resolution (JSON):
    - [x] Decision (APPROVE, REJECT, ESCALATE)
    - [x] Reason
    - [x] Evidence
    - [x] Notes

#### 4.2 Case Management Service
- [x] Create `ComplianceCaseService`
  - [x] Create case from screening results
  - [x] Assign case to compliance officer
  - [x] Track case status and history
  - [x] Add notes and evidence
  - [x] Resolve cases with decisions
  - [x] Generate case reports

#### 4.3 Case Assignment Logic
- [x] Create `CaseAssignmentService` (Implemented as part of `WorkflowAutomationService`)
  - [x] Auto-assign based on:
    - [x] Case type
    - [x] Risk level
    - [x] Officer workload (Simple round-robin/priority)
    - [ ] Officer expertise
  - [x] Manual reassignment
  - [ ] Escalation rules

---

### Phase 5: Ongoing Monitoring

#### 5.1 Continuous Screening Service
- [x] Create `ContinuousScreeningService` (`PeriodicRescreeningService`)
  - [x] Schedule periodic re-screening
  - [x] Monitor for changes in:
    - [x] Sanctions lists
    - [x] PEP status
    - [x] Adverse media
  - [x] Trigger alerts on new matches
  - [x] Update risk scores

#### 5.2 Monitoring Configuration
- [x] Add monitoring settings to `model_config`
  - [x] Screening frequency (daily, weekly, monthly)
  - [x] Alert thresholds
  - [x] Auto-escalation rules

#### 5.3 Monitoring Data Model
- [x] Create `monitoring_alerts` table
  - [x] Alert ID (primary key)
  - [x] Merchant ID (foreign key)
  - [x] Alert type (NEW_SANCTION, NEW_PEP, ADVERSE_MEDIA, RISK_CHANGE)
  - [x] Alert severity (INFO, WARN, CRITICAL)
  - [x] Alert details (JSON)
  - [x] Created at
  - [x] Acknowledged by
  - [x] Acknowledged at
  - [x] Resolution

---

### Phase 6: REST API Endpoints

#### 6.1 Merchant Management APIs
- [x] `POST /api/v1/merchants/onboard` - Onboard new merchant
- [x] `GET /api/v1/merchants/{merchantId}` - Get merchant details
- [x] `PUT /api/v1/merchants/{merchantId}` - Update merchant
- [x] `GET /api/v1/merchants` - List all merchants (with filters)
- [x] `PUT /api/v1/merchants/{merchantId}/status` - Update merchant status

#### 6.2 Beneficial Owner APIs
- [x] `POST /api/v1/merchants/{merchantId}/owners` - Add beneficial owner
- [x] `GET /api/v1/merchants/{merchantId}/owners` - List owners
- [x] `PUT /api/v1/merchants/{merchantId}/owners/{ownerId}` - Update owner
- [x] `DELETE /api/v1/merchants/{merchantId}/owners/{ownerId}` - Remove owner

#### 6.3 Screening APIs
- [x] `POST /api/v1/screening/merchant/{merchantId}` - Screen merchant
- [x] `POST /api/v1/screening/owner/{ownerId}` - Screen beneficial owner
- [x] `GET /api/v1/screening/merchant/{merchantId}/results` - Get screening results
- [x] `GET /api/v1/screening/owner/{ownerId}/results` - Get owner screening results

#### 6.4 Risk Assessment APIs
- [x] `GET /api/v1/risk/merchant/{merchantId}` - Get merchant risk score
- [x] `POST /api/v1/risk/merchant/{merchantId}/recalculate` - Recalculate risk
- [x] `GET /api/v1/risk/high-risk` - List high-risk merchants

#### 6.5 Compliance Case APIs
- [x] `POST /api/v1/cases/create` - Create compliance case
- [x] `GET /api/v1/cases/{caseId}` - Get case details
- [x] `PUT /api/v1/cases/{caseId}` - Update case
- [x] `PUT /api/v1/cases/{caseId}/assign` - Assign case
- [x] `PUT /api/v1/cases/{caseId}/resolve` - Resolve case
- [x] `GET /api/v1/cases` - List cases (with filters)
- [x] `GET /api/v1/cases/my-cases` - Get cases for current officer

#### 6.6 Monitoring APIs
- [x] `GET /api/v1/monitoring/alerts` - Get monitoring alerts
- [x] `PUT /api/v1/monitoring/alerts/{alertId}/acknowledge` - Acknowledge alert
- [x] `GET /api/v1/monitoring/dashboard` - Compliance dashboard metrics

---

### Phase 7: Reporting & Analytics

#### 7.1 Compliance Reporting Service
- [x] Create `ComplianceReportingService`
  - [x] Generate SAR (Suspicious Activity Report) templates
  - [x] Export screening results
  - [x] Generate risk assessment reports
  - [x] Audit trail reports

#### 7.2 Dashboard & Metrics
- [x] Create compliance dashboard endpoints
  - [x] Total merchants by risk level
  - [x] Open cases by priority
  - [x] Screening statistics
  - [x] Alert statistics
  - [x] Officer workload
  - [x] Screening turnaround times

---

### Phase 8: Workflow Automation

#### 8.1 Automated Workflows
- [x] Create `WorkflowAutomationService`
  - [x] Auto-approve low-risk merchants
  - [x] Auto-create cases for high-risk (Handled in `MerchantOnboardingService`)
  - [x] Auto-assign cases based on rules
  - [x] Auto-escalate overdue cases (Implemented via @Scheduled job)
  - [x] Scheduled re-screening jobs (Implemented in `PeriodicRescreeningService`)

#### 8.2 Notification Service
- [x] Create `NotificationService`
  - [x] Email notifications for:
    - [x] Case assignments
    - [x] High-risk alerts
    - [ ] Overdue cases
    - [x] Screening results
  - [x] Webhook notifications for external systems (Implemented in Phase 12)
  - [x] Slack/Teams integration (optional) (Implemented via SlackService)

---

### Phase 9: Enhanced Features

#### 9.1 Advanced Due Diligence
- [x] Create `EnhancedDueDiligenceService`
  - [x] Request additional documentation
  - [x] Source of funds verification
  - [x] Source of wealth verification
  - [x] Business activity verification
  - [ ] Reference checks

#### 9.2 Document Management
- [x] Create `DocumentManagementService`
  - [x] Upload and store merchant documents
  - [x] Document verification status
  - [x] Document expiry tracking (Implemented in Phase 17)
  - [x] Secure document storage (Local FS implementation)

#### 9.3 Transaction Limit Management
- [x] Create `TransactionLimitService`
  - [x] Set merchant transaction limits based on risk
  - [x] Monitor against limits
  - [x] Alert on limit breaches
  - [x] Temporary limit adjustments

---

## Technical Infrastructure Enhancements

### Database Enhancements
- [x] Add merchant-specific indexes (Implemented in Phase 18)
- [x] Implement database partitioning for large tables (SQL Script created in Phase 22)
- [x] Add materialized views for reporting

### Caching Strategy
- [x] Cache merchant risk scores (Redis/Aerospike) (Implemented via Spring Cache + Aerospike)
- [x] Cache screening results (with TTL) (Enabled via @Cacheable)
- [x] Cache AML provider responses (Cached in SumsubAmlService)

### Security & Compliance
- [x] API authentication and authorization (Basic Auth implemented)
- [x] Role-based access control (RBAC) (Implemented in Phase 19)
  - Roles: ADMIN, COMPLIANCE_OFFICER, ANALYST, AUDITOR
- [x] Audit logging for all compliance actions (Partial via AuditTrail)
- [x] Data encryption at rest and in transit (EncryptionService)
- [x] PII data masking (PiiMaskingSerializer)

### Performance Optimization
- [x] Batch screening for bulk onboarding (BatchScreeningController)
- [x] Async screening with webhooks (SumsubAmlService)
- [x] Rate limiting for AML provider API calls (Resilience4j implemented)
- [ ] Connection pooling for AML provider

---

## Configuration Requirements

### New Configuration Properties
```properties
# Merchant Screening Configuration
merchant.screening.enabled=true
merchant.screening.auto.onboarding.enabled=false
merchant.screening.batch.size=50

# Risk Scoring Thresholds
risk.scoring.low.threshold=30
risk.scoring.medium.threshold=60
risk.scoring.high.threshold=80

# Case Management
case.assignment.auto.enabled=true
case.escalation.days=7
case.due.days.low=30
case.due.days.medium=14
case.due.days.high=7
case.due.days.urgent=2

# Ongoing Monitoring
monitoring.frequency.days=30
monitoring.auto.screening.enabled=true

# Notifications
notifications.email.enabled=false
notifications.webhook.enabled=false
```

---

## Migration Strategy

### Step 1: Database Schema
1. Create new merchant-related tables
2. Add foreign key relationships
3. Create indexes
4. Migrate any existing merchant data

### Step 2: Core Services
1. Implement merchant data model
2. Create onboarding service
3. Integrate AML provider
4. Build risk scoring engine

### Step 3: API Endpoints
1. Implement merchant management APIs
2. Add screening APIs
3. Build case management APIs

### Step 4: Testing
1. Unit tests for all services
2. Integration tests with AML provider
3. Load testing for high volume
4. Security testing

### Step 5: Deployment
1. Deploy to staging
2. Test with sample merchant data
3. User acceptance testing
4. Production deployment

---

## Success Metrics

### Operational Metrics
- [ ] Merchant onboarding time < 24 hours (low risk)
- [ ] Screening success rate > 99%
- [ ] Case resolution time < 7 days (medium risk)
- [ ] System uptime > 99.9%
- [ ] API response time < 200ms (p95)

### Compliance Metrics
- [ ] False positive rate < 10%
- [ ] Detection rate for sanctions matches = 100%
- [ ] PEP detection rate > 95%
- [ ] Audit trail completeness = 100%

### Business Metrics
- [ ] Customer satisfaction > 4.0/5.0
- [ ] Compliance cost reduction > 30%
- [ ] Manual review time reduction > 50%

---

## Timeline (Estimated)

- **Phase 1-2 (Months 1-2):** Merchant data model + AML integration
- **Phase 3-4 (Month 3):** Risk scoring + Case management
- **Phase 5-6 (Month 4):** Monitoring + APIs
- **Phase 7-8 (Month 5):** Reporting + Automation
- **Phase 9 (Month 6):** Enhanced features + Production deployment

---

## Next Immediate Steps

1. [x] Finalize AML service provider selection
2. [x] Design merchant database schema
3. [x] Create implementation plan for Phase 1
4. [x] Set up development environment for merchant module
5. [x] Create merchant onboarding API specification

---

**Last Updated:** 2025-12-29
**Status:** Implementation Complete
**Next Review:** Deployment Readiness Review
