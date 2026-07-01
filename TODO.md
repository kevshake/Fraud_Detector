# TODO — Full Platform Completion (no stubs, no mocks, no placeholders)
_Last updated: 2026-06-29_

---

## Wave 13 — End-to-End Wiring Completion (this session) ✅

### W13-A: Usage Tracking → Billing Pipeline (CRITICAL GAP FIX) ✅

- [x] **UsageTrackingFilter** — Created `UsageTrackingFilter.java` (OncePerRequestFilter) intercepts every `/api/v1/*` request.
- [x] Resolves service type from URL pattern (12 mappings: TRANSACTION_PROCESSING, SANCTIONS_SCREENING, AML_CHECK, MERCHANT_ONBOARDING, REPORT_GENERATION, CASE_MANAGEMENT, ALERT_MANAGEMENT, SAR_FILING, CBK_REPORTING, BILLING_OPERATIONS, etc.).
- [x] Extracts PSP ID from SecurityContext and fires `ApiUsageTrackingService.logRequest()` asynchronously.
- [x] **Critical fix**: `ApiUsageTrackingService.logRequest()` was NEVER called from anywhere — billing pipeline was completely dormant. The filter fixes this.
- [x] Cost per transaction calculated in real-time via `BillingService.calculateUsageCost(pspId, serviceType, 1)`.
- [x] PSP consumption now flows: API → UsageTrackingFilter → ApiUsageLog → Invoice Generation.

### W13-B: Hardcoded Country/MCC Risk Lists → Config-Driven ✅

- [x] **AmlCheckService (microservice)** — `HIGH_RISK_COUNTRIES`/`MEDIUM_RISK_COUNTRIES` static Set.of() replaced with `@Value`-injected fields from `application.yml`.
- [x] **FraudDetectionService** — FATF_HIGH_RISK_COUNTRIES static Set replaced with `@Value`-injected `fallbackHighRiskCountries` (configurable per env).
- [x] **AmlService** — Same treatment via `aml.fallback-high-risk-countries`.
- [x] **RiskScoringService** — MCC_RISK static HashMap replaced with `MccRiskConfig @ConfigurationProperties(prefix="risk.mcc")` class.
- [x] All defaults preserve existing FATF/MCC values.

### W13-C: DB-Backed Country Risk Management Controllers ✅

- [x] **HighRiskCountryController** (`/api/v1/risk/high-risk-countries`) — Full CRUD (GET list, GET/{id}, POST, PUT/{id}, DELETE/{id}). DB-backed `high_risk_countries` table. Access: ADMIN/COMPLIANCE_OFFICER manage, all authenticated read.
- [x] **CountryRiskScoreController** (`/api/v1/risk/country-scores`) — Full CRUD for `country_risk_scores` table (numeric scores, FATF tiers, blacklist/greylist status).
- [x] Primary data source for `FraudDetectionService.isHighRiskCountry()`, `AmlService.assessGeographicRisk()`, `RiskScoringService.getCountryRisk()`.

### W13-D: Alert → Case Escalation Bridge ✅

- [x] **AlertToCaseService** — Auto-creates `ComplianceCase` when alerts resolved with case-worthy dispositions (ESCALATED, TRUE_POSITIVE_* , MERGED_WITH_CASE, PENDING_INFORMATION, ONGOING_MONITORING).
- [x] **Escalation Matrix**:
  - Score ≥0.9 → CRITICAL (4h SLA) → immediate escalation to MLRO
  - Score ≥0.7 → HIGH (24h SLA) → escalate to COMPLIANCE_OFFICER
  - Score ≥0.4 → MEDIUM (3d SLA) → assign to ANALYST
  - ELSE → LOW (7d SLA) → assign to ANALYST
- [x] Decision outcomes: APPROVE→CLOSED_CLEARED, REJECT→CLOSED_BLOCKED, FILE_SAR→CLOSED_SAR_FILED.
- [x] Auto-assign via `WorkflowAutomationService` (workload balance).
- [x] Auto-escalation check via `CaseEscalationService.checkAutomaticEscalation()`.
- [x] Wired into `AlertController.resolveAlert()`.

### W13-E: CBK Reporting — Full Real-Data Audit + Fraud Incident Auto-Population ✅

- [x] **AlertFraudIncidentBridge** — Auto-creates `PspFraudIncident` when alert disposed TRUE_POSITIVE (BLOCKED/SAR_FILED/REPORTED). Feeds daily CBK GDI FRAUD_INCIDENTS endpoint from real alert data.
- [x] **Verified all 17 CBK GDI endpoints use real DB data**:
  - Annual (4): SENIOR_MANAGEMENT, DIRECTORS, TRUSTEES, SHAREHOLDERS → Psp*Repository
  - Monthly (5): CUSTOMER_COMPLAINTS, PRODUCTS_INFO, CARD_BRANDS, TRANSACTION_DETAILS, TRANSACTION_TARIFFS → Psp*Repository + TransactionRepository
  - Daily (8): CYBER_INCIDENT, FRAUD_INCIDENTS, SYSTEM_STABILITY, SYSTEM_ACTIVITY, TRUST_ACCOUNT, BILLING_TEMPLATE, MERCHANT_TRANSACTIONS, FAILED_TRANSACTIONS → Psp*Repository + TransactionRepository

### W13-F: Cross-PSP Fraud Intelligence ✅

- [x] **CrossPspFraudFlag entity** + Flyway V146 migration (`cross_psp_fraud_flags` table). Tracks MERCHANT_ID, PAN_HASH, TERMINAL, and NAME flags.
- [x] **CrossPspFraudIntelligenceService**:
  - WRITE: Auto-populated from TRUE_POSITIVE alerts via `AlertFraudIncidentBridge`. Flags merchant ID, PAN hash, terminal, merchant trading name. Uses `NameMatchingService` (DoubleMetaphone + Levenshtein) for fuzzy name matching (≥80% similarity). Escalates risk level on repeat cross-PSP flags.
  - READ: `screenTransaction(txn)` — exact match + fuzzy name match against flagged entities. BLOCK if high risk or multi-PSP; HOLD otherwise.
- [x] **DecisionEngine integration**: 2nd hard rule (after sanctions, before ML scoring).

### W13-G: Dashboard Analytics Refresh Scheduler ✅

- [x] **DashboardAnalyticsRefresher** + **DashboardCache** — Recalculates live dashboard KPIs every 4 minutes (`0 */4 * * * *`). Conditionally disabled via `dashboard.analytics.refresh.enabled=false`.

### W13-H: Report Export Verification ✅

- [x] **All exports verified real**: ReportExportService (PDF/CSV/XML), ReportGenerationService (native SQL), DashboardController (real aggregates), RiskAnalyticsController, TransactionMonitoringController.

---

## Wave 12 — Dashboard redesign (Tailwind + Shadcn migration) 🚧

Pixel-match the Hokeka AML mockup; full migration off MUI to Tailwind + Shadcn + lucide + framer-motion + recharts + react-simple-maps.

- [x] **#69** Phase 1 — Tailwind+Shadcn foundation (deps, tailwind.config, CSS vars, Inter font, cn helper)
- [x] **#70** Phase 2 — DashboardLayout: 280px dark sidebar (#07142E), grouped nav, Hokeka SVG logo, header
- [x] **#71** Phase 3 — Dashboard widgets: 6 KPI cards, Risk Gauge, Live Alert Queue, Risk Heatmap, Investigation Cases donut, Alert Trends, Screening Results, Top Risk Merchants, Compliance Health
  - [x] Create `src/hooks/useDashboard.ts` with typed React Query hooks
  - [x] Build `src/components/kpi/KpiCard.tsx`
  - [x] Build `src/components/charts/RiskGauge.tsx`
  - [x] Build `src/components/Alerts/LiveAlertQueue.tsx`
  - [x] Build `src/components/charts/RiskHeatmap.tsx`
  - [x] Build `src/components/Cases/InvestigationCases.tsx`
  - [x] Build `src/components/charts/AlertTrends.tsx`
  - [x] Build `src/components/Alerts/ScreeningResults.tsx`
  - [x] Build `src/components/Alerts/TopRiskMerchants.tsx`
  - [x] Build `src/components/compliance/ComplianceHealth.tsx`
  - [x] Replace `src/pages/Dashboard/DashboardPage.tsx`
  - [x] `npm run typecheck` passes with zero errors
- [x] **#72** Phase 4 — Backend aggregate endpoints (extended `/dashboard/stats` + new `/risk-heatmap`, `/cases/closed-recent`, `/screening/results-today`, `/merchants/top-risk`, `/compliance/health`, `/alerts/trends`); V140 migration for CDD/EDD review timestamps; all widgets now consume real data
- [ ] **#73** Phase 5 — Migrate remaining pages off MUI, drop @mui/material (long tail — separate session)
  - [x] Phase 5a — Shared Tailwind component library (TwTable, TwPagination, TwBadge, TwSnackbar, TwInput, TwSelect)
  - [x] Phase 5b — AlertsPage, AuditLogsPage, MerchantsPage, KycDocumentsPage migrated (4 pages)
  - [x] Phase 5c — TransactionMonitoringLive/Reports/Sars migrated (3 pages)
  - [ ] Phase 5d — Remaining 15+ pages + AnalyticsPage + ReportsCenter components

---

## Wave 11 — Frontend cleanup (Task #68) ✅

- [x] **#68** Deleted orphaned `RolesPage.tsx` + `pages/Roles/` dir; added `/limits-aml` sidebar nav link; removed 7 empty page dirs; `npm run typecheck` passes clean.

---

## Wave 10 — Stub fixes (Task #67) ✅

- [x] **#67A** CBK controllers hardened: all 11 PSP CBK controllers + `CbkReportController` rewritten — NPE-safe `getCurrentUser()`/`getCurrentPspId()`.
- [x] **#67B** `PeriodicSanctionsScreeningService` — placeholder removed; new `CaseCreationService.triggerCaseFromSanctionsForMerchant()`.
- [x] **#67C** `SarContentGenerationService` — `MerchantRepository` injected; placeholder fallbacks removed.
- [x] **#67D** `mvn clean compile` BUILD SUCCESS — 648 sources compile.

---

## Wave 9 — Reports, Analytics, Data Output + DB Indexes ✅

- [x] **#59** `DashboardController` sanctions status + fraud metrics — all real DB queries.
- [x] **#60** `ReportExportService` — complete rewrite: real OpenPDF PDF, real CSV, no file-system writes.
- [x] **#61** `RegulatoryReportingService` — indexed queries, PSP-scoped stats endpoint.
- [x] **#62** `AnalyticsPage.tsx` — native Recharts analytics (4 tabs), Grafana fallback.
- [x] **#63** `V135__production_performance_indexes.sql` — 26 new composite/partial indexes.

---

## Wave 8 — Zero Stubs: Fraud Scoring, Risk, Kafka, Compliance ✅

- [x] **#53** `FraudDetectionService` — replaced 3 hardcoded-0 stubs (device/IP/behavioral risk).
- [x] **#54** `RiskScoringService.calculateCra()` — 5-dimension weighted CRA.
- [x] **#55** `FeatureExtractionService` — proper EMV CVMR 3-byte parsing.
- [x] **#56** `ComplianceReportingService.generateFincenXml()` — full goAML-pattern SAR XML.
- [x] **#57** UserService, WorkflowAutomationService, PrometheusMetricsService — all real DB/connections.
- [x] **#58** Kafka expanded 3→8 topics, ingestion pipeline end-to-end.

---

## Wave 7 — PSP Self-Serve Billing + Payments ✅

- [x] **#51** `SettingsPage.tsx` — PSP users see billing tab; platform admins see Theme + System Settings.
- [x] **#52** M-Pesa payment capability end-to-end (Daraja STK Push, callback, invoice auto-mark PAID).

---

## Wave 6 — SaaS Billing (full end-to-end) ✅

- [x] **#47** Expanded `BillingController` (+7 endpoints) + `SubscriptionController` (7 CRUD endpoints).
- [x] **#48** `InvoicePdfService` (OpenPDF A4 branded PDF), `BillingEmailService`, `DunningScheduler`.
- [x] **#49** `BillingPage.tsx` (admin, 4 tabs): Revenue Dashboard, Subscriptions, Invoices, Usage.
- [x] **#50** `BillingTab.tsx` added to `PspConfigPage` (tab #9): plan, usage, invoice history.

---

## Done (from previous sessions)

- [x] CBK GDI API inventory — all 17 endpoints documented (`docs/integrations/CBK_API_INVENTORY.md`)
- [x] PSP entity extended — CBK fields, directors, shareholders, trustees, senior mgmt, products, trust accounts, tariffs, cyber/system incidents, complaints, fraud incidents
- [x] Flyway V124–V132 — all CBK, classification columns, deferred tables, demo seeds
- [x] `CbkGdiClient` — 17 submit methods, circuit-breaker/retry, absolute URL routing
- [x] `CbkTokenService` — per-(pspId,env) token cache, AES-GCM encrypted credentials
- [x] `CbkSubmissionOrchestrator` — all 17 endpoints wired end-to-end, date-windowed queries
- [x] `CbkScheduler` — 6 cron jobs (daily/monthly/annual), conditional on `cbk.enabled`
- [x] PSP-scoped live lock — 3-guard: global `cbk.allow-live` + per-PSP `cbkAllowLive` + per-PSP `cbkEnvironment`
- [x] `PspController` GET/PUT `/psps/{id}/cbk-config` — ADMIN-only promotion, PSP_ADMIN read-only
- [x] Frontend `CbkReportingTab` — environment promotion panel (admin only), live badge, credential fields
- [x] Frontend `PspsListPage` + `PspConfigPage` (9 tabs) — full PSP management UI
- [x] Frontend `CbkSubmissionsTab` — paginated submission history with replay button
- [x] Demo seed accounts — SUPER_ADMIN, ADMIN, 3 PSPs, PSP_ADMIN + PSP_USER per PSP
- [x] AES-GCM encryption at rest — `registrationNumber`, `taxId`, `cbkClientId`, `cbkClientSecret`
- [x] GitHub sync — all code pushed to origin/main @ `7bb46159`