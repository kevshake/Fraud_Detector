# AML/Fraud Detector – Reporting & UI Guide

## Overview
This guide covers:
- Reporting endpoints and available metrics (including graph-ready data).
- Roles and permissions.
- How to navigate the application UI (by role).

## Roles & Permissions (Dynamic RBAC)

The system now uses a **dynamic Role-based Access Control (RBAC)** system. Roles are no longer hardcoded but can be created and managed dynamically via the UI.

### System Roles (Default)
- **ADMIN**: Global system administrator with all permissions (`MANAGE_USERS`, `MANAGE_ROLES`, `MANAGE_PSP`, etc.).
- **VIEWER**: Read-only access to basic dashboards.

### PSP-Scoped Roles
Roles can be created specifically for a PSP (e.g., "TechFlow Analyst") and are isolated to that PSP's data.

### Permissions
Permissions are granular and include:
- `MANAGE_USERS`, `MANAGE_ROLES`
- `CREATE_CASES`, `ASSIGN_CASES`, `CLOSE_CASES`
- `APPROVE_SAR`, `FILE_SAR`
- `VIEW_PII`, `VIEW_AUDIT_LOGS`

### API for Auth & Roles
- `GET /api/v1/users` - List users (scoped to PSP)
- `GET /api/v1/roles` - List roles (scoped to PSP)
- `GET /api/v1/auth/permissions` - List all available permissions

## UI Navigation & Dashboards

### 1. Main Dashboard
- **Stats**: Risk breakdown, SAR performance, System overview.
- **Charts**: Interactive transaction volume charts.
- **Recent Cases**: Quick view of latest cases.

### 2. User Management
- **List Users**: View all users within your scope.
- **Add User**: Create new users, assigning them to a Role and PSP.
- **Edit/Delete**: Manage existing user accounts.

### 3. Role Management
- **Create Role**: Define new roles with a custom set of permissions.
- **PSP Awareness**: Roles can be Global (System) or PSP-specific.

## Reporting Endpoints
Base: `/api/v1/reporting`
- `GET /summary`  
  Returns:  
  - `casesByStatus`: map of CaseStatus -> count  
  - `sarsByStatus`: map of SarStatus -> count  
  - `auditLast24h`: total audit events in last 24h  
  - `casesLast7d`: daily counts (date -> count)  
  - `sarsLast7d`: daily counts (date -> count)
- `GET /cases/status` -> CaseStatus counts
- `GET /sars/status` -> SarStatus counts
- `GET /cases/daily?days=7` -> daily counts for cases
- `GET /sars/daily?days=7` -> daily counts for SARs
- `GET /audit/last24h` -> audit events in last 24h

Graphing: The daily endpoints return `{ "YYYY-MM-DD": count }` which can be plotted directly as time-series.

## Workflow Controllers
- Case workflow: `/api/v1/compliance/cases/workflow`
  - POST `/create` (caseReference, description, priority, creatorUserId)
  - POST `/assign` (caseId, assigneeUserId, assignerUserId)
  - POST `/status` (caseId, status, userId)
  - POST `/escalate` (caseId, escalatedToUserId, reason, userId)
- SAR workflow: `/api/v1/compliance/sar/workflow`
  - POST `/create` (sarReference, narrative, suspiciousActivityType, jurisdiction, sarType, creatorUserId)
  - POST `/submit` (sarId, userId)
  - POST `/approve` (sarId, userId)
  - POST `/reject` (sarId, userId, reason)
  - POST `/file` (sarId, userId, filingReference)

Audit logs:
- `/api/v1/audit/logs/entity?entityType=CASE&entityId=123`
- `/api/v1/audit/logs/user/{username}`
- `/api/v1/audit/logs/range?start=2025-01-01T00:00:00&end=2025-01-02T00:00:00`

## Navigation by Role (suggested UI flows)
- ADMIN: access all menus (cases, SARs, screening, users, audit). Can manage roles/permissions.
- MLRO: SAR approvals/filing, close cases, see audit. Approve high-risk actions.
- COMPLIANCE_OFFICER: create/assign cases, submit/approve SARs (as per policy), view PII/screening, manage watchlists.
- INVESTIGATOR: view/annotate cases, add evidence/notes, submit SAR for review. No approvals.
- ANALYST: view cases, add internal notes. Limited PII.
- SCREENING_ANALYST: screening results, overrides/whitelisting. Limited case access.
- CASE_MANAGER: assign/reopen/close (where policy allows), monitor queues.
- AUDITOR: read-only access to audit/cases/SARs; no modifications.
- VIEWER: minimal read-only views.

## How it works (high level)
- Ingestion → Feature extraction → Scoring → Decision → Case/SAR workflows.
- High-throughput configuration (Tomcat, async pools, Redis/Aerospike init).
- Dynamic HTTP/2 failover, connection pooling, and Redis/Aerospike initialization.
- Audit logging for critical actions (case and SAR workflows currently instrumented).

## Running Reports
1) Call `/api/v1/reporting/summary` for a quick dashboard.
2) Plot daily series from `/cases/daily` and `/sars/daily` on your UI charts.
3) Use audit endpoints to drive compliance dashboards (activity in last 24h, by user, by entity).

## Notes
- All configs are externalized in `application.properties`.
- Database migrations for cases/SARs/audit are in `src/main/resources/db/migration/V4__compliance_sar_audit.sql`.
- Extend reporting by adding more repository filters as needed (e.g., by merchant/PSP/priority).

