# API Reference Document
## AML Fraud Detector System

**Version:** 1.0  
**Base URL:** `http://localhost:2637`  
**API Version:** v1

---

## 1. Overview

### 1.1 API Standards
- **Protocol**: REST over HTTP/HTTPS
- **Content-Type**: `application/json`
- **Authentication**: Session-based authentication with strict session isolation
  - Login via `POST /api/v1/auth/login` to create a session
  - Session cookies are automatically managed by the browser
  - All authenticated requests require valid session cookies
  - Session timeout: 30 minutes (configurable)
- **PSP ID Tracking**: All API requests include `pspId` query parameter
  - Super Admin users: `pspId=0` (SYSTEM_ADMIN PSP)
  - PSP users: `pspId={their_psp_id}` (their assigned PSP ID)
  - Backend automatically filters data based on PSP ID
  - Frontend automatically includes `pspId` in all requests
- **Security Features**:
  - Session fixation protection
  - Cross-user data access prevention
  - Session ownership validation
  - Complete session cleanup on logout
  - PSP-based data isolation enforced at API level
- **Documentation**: Swagger UI at `/swagger-ui.html`

### 1.2 Response Format

#### Success Response
```json
{
    "success": true,
    "data": { ... },
    "timestamp": "2026-01-09T08:00:00Z"
}
```

#### Error Response
```json
{
    "timestamp": "2026-01-09T08:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "errorCode": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": ["field: error description"],
    "traceId": "abc-123-xyz"
}
```

### 1.3 HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request succeeded |
| 201 | Created - Resource created |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Authentication required |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource not found |
| 500 | Internal Server Error |

---

## 2. Authentication & Session Management APIs

### 2.1 Login

Authenticate a user and create a session.

**Endpoint:** `POST /api/v1/auth/login`

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
    "username": "admin",
    "password": "password"
}
```

**Response (200 OK):**
```json
{
    "success": true,
    "token": "ABC123XYZ789",
    "redirectUrl": "/dashboard",
    "user": {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "firstName": "Admin",
        "lastName": "User",
        "enabled": true,
        "createdAt": "2026-01-01T00:00:00",
        "pspId": 0,
        "role": {
            "id": 1,
            "name": "ADMIN",
            "permissions": ["ALL"]
        },
        "psp": {
            "id": 0,
            "name": "System Admin",
            "code": "SYSTEM_ADMIN"
        }
    }
}
```

**Note:** All users have a `pspId`:
- **Super Admin users**: `pspId = 0` (SYSTEM_ADMIN PSP)
- **PSP users**: `pspId > 0` (their assigned PSP ID)

**Error Responses:**
- `400 Bad Request`: Missing username or password
- `401 Unauthorized`: Invalid credentials
- `403 Forbidden`: User account is disabled

**Security Features:**
- Invalidates any existing session to prevent session fixation
- Creates new session with user-specific attributes
- Stores user ID and username in session for validation
- Ensures session isolation per user
- Session timeout: 30 minutes (1800 seconds)

---

### 2.2 Get Current User

Get the currently authenticated user's information.

**Endpoint:** `GET /api/v1/auth/me`

**Authentication:** Required (session-based)

**Response (200 OK):**
```json
{
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "enabled": true,
    "createdAt": "2026-01-01T00:00:00",
    "role": {
        "id": 1,
        "name": "ADMIN",
        "permissions": ["ALL"]
    },
    "psp": {
        "id": 1,
        "name": "Example PSP",
        "code": "PSP001"
    }
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated or session validation failed

**Security Features:**
- Validates authentication from SecurityContext
- Validates session ownership (user ID in session matches authenticated user)
- Prevents cross-user data access
- Only returns data for the authenticated user

---

### 2.3 Update Current User Profile

Update the currently authenticated user's profile information.

**Endpoint:** `PUT /api/v1/users/me`

**Authentication:** Required (session-based)

**Request Body:**
```json
{
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com"
}
```

**Response (200 OK):**
```json
{
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "enabled": true,
    "role": { ... },
    "psp": { ... }
}
```

**Error Responses:**
- `400 Bad Request`: Validation error or email already exists
- `401 Unauthorized`: Not authenticated

---

### 2.4 Change Password

Change the currently authenticated user's password.

**Endpoint:** `PUT /api/v1/users/me/password`

**Authentication:** Required (session-based)

**Request Body:**
```json
{
    "currentPassword": "oldPassword123",
    "newPassword": "newPassword456"
}
```

**Response (200 OK):**
```json
{
    "success": true
}
```

**Error Responses:**
- `400 Bad Request`: Incorrect current password
- `401 Unauthorized`: Not authenticated

### 2.3 Logout

Logout the current user and invalidate the session.

**Endpoint:** `POST /api/v1/auth/logout`

**Authentication:** Required (session-based)

**Response (200 OK):**
```json
{
    "message": "Logged out successfully"
}
```

**Security Features:**
- Clears all session attributes before invalidation
- Clears SecurityContext
- Logs logout event for audit
- Ensures complete session cleanup

---

### 2.4 Check Session

Check if the current session is valid and get remaining time.

**Endpoint:** `GET /auth/session/check` or `GET /api/v1/auth/session/check`

**Authentication:** Required (session-based)

**Response (200 OK):**
```json
{
    "valid": true,
    "timeRemaining": 1650,
    "maxInactiveInterval": 1800,
    "lastAccessedTime": 1704700800000,
    "username": "admin"
}
```

**Error Responses:**
- `401 Unauthorized`: No active session or not authenticated

---

### 2.5 Refresh Session

Refresh the current session to extend its timeout.

**Endpoint:** `POST /auth/session/refresh` or `POST /api/v1/auth/session/refresh`

**Authentication:** Required (session-based)

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Session refreshed",
    "sessionTimeout": 1800,
    "timeRemaining": 1800,
    "username": "admin"
}
```

**Error Responses:**
- `401 Unauthorized`: No active session or not authenticated

---

### 2.6 Get Session Information

Get detailed information about the current session.

**Endpoint:** `GET /auth/session/info`

**Authentication:** Optional (returns info if session exists)

**Response (200 OK):**
```json
{
    "active": true,
    "sessionId": "ABC123XYZ789",
    "maxInactiveInterval": 1800,
    "createdAt": 1704700800000,
    "lastAccessedAt": 1704702450000,
    "username": "admin"
}
```

---

### 2.7 Invalidate Session

Manually invalidate the current session.

**Endpoint:** `POST /auth/session/invalidate`

**Authentication:** Optional

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Session invalidated"
}
```

---

### 2.8 Get CSRF Token

Get CSRF token for form submissions.

**Endpoint:** `GET /auth/csrf` or `GET /api/v1/auth/csrf`

**Authentication:** Not required (public endpoint)

**Response (200 OK):**
```json
{
    "token": "csrf-token-value",
    "headerName": "X-CSRF-TOKEN",
    "parameterName": "_csrf"
}
```

---

### 2.9 Request Password Reset

Request a password reset link via email.

**Endpoint:** `POST /auth/password-reset/request` or `POST /api/v1/auth/password-reset/request`

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
    "identifier": "user@example.com"
}
```

**Response (200 OK):**
```json
{
    "message": "If the account exists, a reset link has been sent."
}
```

**Note:** Always returns 200 OK to prevent user enumeration attacks.

---

### 2.10 Confirm Password Reset

Confirm password reset using a one-time token.

**Endpoint:** `POST /auth/password-reset/confirm` or `POST /api/v1/auth/password-reset/confirm`

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
    "token": "reset-token-from-email"
}
```

**Response (200 OK):**
```json
{
    "message": "Password reset successful. Your password has been reset to the default password. Please check your email for details."
}
```

---

### 2.11 Emergency Password Reset

Emergency password reset for super-user recovery (disabled by default).

**Endpoint:** `POST /auth/password-reset/emergency` or `POST /api/v1/auth/password-reset/emergency`

**Authentication:** Not required (requires special header)

**Request Headers:**
- `X-EMERGENCY-RESET-SECRET`: Secret key (required)

**Request Body:**
```json
{
    "identifier": "admin@example.com"
}
```

**Response (200 OK):**
```json
{
    "message": "If the account exists, the password has been reset to the default password. Please check your email for details."
}
```

**Error Responses:**
- `403 Forbidden`: Emergency reset not enabled or invalid secret

**Note:** This endpoint is disabled by default and requires server-side configuration.

---

### 2.12 Get Roles

Get list of available roles, optionally filtered by PSP.

**Endpoint:** `GET /auth/roles`

**Authentication:** Required

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| pspId | Long | Optional PSP ID to filter roles |

**Response (200 OK):**
```json
[
    {
        "id": 1,
        "name": "ADMIN",
        "description": "System Administrator",
        "permissions": ["ALL"]
    },
    {
        "id": 2,
        "name": "PSP_ADMIN",
        "description": "PSP Administrator",
        "permissions": ["MANAGE_USERS", "VIEW_REPORTS"]
    }
]
```

---

### 2.13 Get Permissions

Get list of all available permissions.

**Endpoint:** `GET /auth/permissions`

**Authentication:** Required

**Response (200 OK):**
```json
[
    "ALL",
    "MANAGE_USERS",
    "VIEW_REPORTS",
    "MANAGE_CASES",
    "VIEW_ALERTS"
]
```

---

### 2.14 Get Role Permissions

Get permissions for a specific role.

**Endpoint:** `GET /auth/role-permissions`

**Authentication:** Required

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| roleId | Long | Role ID (required) |

**Response (200 OK):**
```json
[
    "ALL",
    "MANAGE_USERS",
    "VIEW_REPORTS"
]
```

**Error Responses:**
- `400 Bad Request`: Role not found

---

## 3. Transaction APIs

### 2.1 Ingest Transaction

Process a new transaction through the fraud detection pipeline.

**Endpoint:** `POST /api/v1/transactions/ingest`

**Request Body:**
```json
{
    "isoMsg": "ISO 8583 message (optional)",
    "pan": "4242424242424242",
    "merchantId": "MERCH-001",
    "terminalId": "TERM-001",
    "amountCents": 10000,
    "currency": "USD",
    "txnTs": "2026-01-09T10:30:00",
    "emvTags": {
        "9F34": "020000",
        "82": "3F00",
        "OF": "A0000000041010"
    },
    "direction": "INBOUND"
}
```

**Response (200 OK):**
```json
{
    "txnId": 12345,
    "score": 0.85,
    "action": "HOLD",
    "reasons": [
        "Score 0.850 >= hold threshold 0.700"
    ],
    "latencyMs": 45,
    "scores": {
        "mlScore": 0.85,
        "krsScore": 65.5,
        "trsScore": 59.5,
        "craScore": 63.75,
        "anomalyScore": 0.015,
        "fraudScore": 30.0,
        "amlScore": 70.0
    }
}
```

| Field | Type | Description |
|-------|------|-------------|
| txnId | Long | Assigned transaction ID |
| score | Double | Primary fraud score (0.0 - 1.0) - ML score or rule override |
| action | String | BLOCK, HOLD, ALERT, or ALLOW |
| reasons | Array | List of triggered rules |
| latencyMs | Long | Processing time in ms |
| scores | Object | **All calculated scores** (see Score Details below) |

**Score Details:**
- `mlScore`: Machine Learning score (0.0-1.0) from XGBoost model
- `krsScore`: KYC Risk Score (0-100) - customer/merchant profile risk
- `trsScore`: Transaction Risk Score (0-100) - transaction-specific risk
- `craScore`: Customer Risk Assessment (0-100) - evolving customer risk profile
- `anomalyScore`: Anomaly detection score (0.0-1.0) - reconstruction error
- `fraudScore`: Fraud detection score (0-100+) - rule-based points
- `amlScore`: AML risk score (0-100+) - AML rule-based points

**Note:** For detailed score calculation formulas and examples, see **[SCORING_PROCESS_DOCUMENTATION.md](../SCORING_PROCESS_DOCUMENTATION.md)**.

---

### 2.2 Get Transactions

Retrieve list of transactions with filtering.

**Endpoint:** `GET /api/v1/transactions`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 0) |
| size | int | Page size (default: 20) |
| merchantId | String | Filter by merchant |
| startDate | String | Start date (ISO 8601) |
| endDate | String | End date (ISO 8601) |
| decision | String | Filter by decision |

**Response (200 OK):**
```json
{
    "content": [
        {
            "id": 12345,
            "panHash": "abc123...",
            "merchantId": "MERCH-001",
            "amountCents": 10000,
            "currency": "USD",
            "fraudScore": 0.25,
            "decision": "ALLOW",
            "createdAt": "2026-01-09T10:30:00"
        }
    ],
    "totalElements": 1500,
    "totalPages": 75,
    "number": 0
}
```

---

### 2.3 Get Transaction Details

**Endpoint:** `GET /api/v1/transactions/{id}`

**Response (200 OK):**
```json
{
    "id": 12345,
    "panHash": "abc123...",
    "merchantId": "MERCH-001",
    "terminalId": "TERM-001",
    "amountCents": 10000,
    "currency": "USD",
    "txnTimestamp": "2026-01-09T10:30:00",
    "emvTags": { ... },
    "fraudScore": 0.85,
    "decision": "HOLD",
    "reasons": ["Score exceeded hold threshold"],
    "features": { ... },
    "createdAt": "2026-01-09T10:30:00",
    "direction": "INBOUND",
    "merchantCountry": "KEN"
}
```

---

## 4. Alert APIs

### 3.1 List Alerts

**Endpoint:** `GET /api/v1/alerts`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| status | String | OPEN, INVESTIGATING, RESOLVED |
| priority | String | LOW, MEDIUM, HIGH, CRITICAL |
| page | int | Page number |
| size | int | Page size |

**Response (200 OK):**
```json
{
    "content": [
        {
            "id": 100,
            "type": "HIGH_FRAUD_SCORE",
            "priority": "HIGH",
            "status": "OPEN",
            "transactionId": 12345,
            "merchantId": "MERCH-001",
            "description": "Fraud score 0.92 exceeded threshold",
            "createdAt": "2026-01-09T10:35:00"
        }
    ],
    "totalElements": 50
}
```

---

### 3.2 Get Alert Details

**Endpoint:** `GET /api/v1/alerts/{id}`

---

### 3.3 Resolve Alert

**Endpoint:** `PUT /api/v1/alerts/{id}/resolve`

**Request Body:**
```json
{
    "disposition": "FALSE_POSITIVE",
    "notes": "Customer verified transaction"
}
```

**Disposition Values:** `FALSE_POSITIVE`, `CONFIRMED_FRAUD`, `ESCALATED`

---

### 3.4 Get Active Alert Count

Get the count of active (non-resolved) alerts filtered by PSP.

**Endpoint:** `GET /api/v1/alerts/count/active`

**Authentication:** Required (session-based)

**PSP Filtering:**
- Super Admin (PSP ID 0): Sees count of all active alerts across all PSPs
- PSP users: See only their PSP's active alert count

**Response (200 OK):**
```json
{
    "count": 42
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated

---

### 3.5 Delete Alert

Delete an alert by ID with PSP access validation.

**Endpoint:** `DELETE /api/v1/alerts/{id}`

**Authentication:** Required (session-based)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | Alert ID to delete |

**PSP Security:**
- Super Admin: Can delete any alert
- PSP users: Can only delete alerts belonging to their PSP

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Alert deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: User does not have permission to delete this alert (PSP mismatch)
- `404 Not Found`: Alert not found

---

### 3.6 Alert Disposition Statistics

Get alert disposition statistics with optional time filtering.

**Endpoint:** `GET /api/v1/alerts/disposition-stats`

**Authentication:** Required (session-based)

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| days | Integer | Optional. Number of days to look back (e.g., 7, 30, 90). If not provided, returns all-time stats |

**PSP Filtering:**
- Super Admin (PSP ID 0): Sees stats for all alerts across all PSPs
- PSP users: See only their PSP's alert statistics

**Response (200 OK):**
```json
{
    "totalAlerts": 1000,
    "falsePositives": 650,
    "confirmedFraud": 150,
    "escalated": 100,
    "pending": 100,
    "falsePositiveRate": 0.65,
    "period": "Last 30 days"
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated

---

## 5. Case Management APIs

### 4.1 List Cases

**Endpoint:** `GET /api/v1/compliance/cases`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| status | String | NEW, ASSIGNED, INVESTIGATING, etc. |
| assignedTo | Long | Analyst user ID |
| priority | String | LOW, MEDIUM, HIGH |

---

### 4.2 Get Case Details

**Endpoint:** `GET /api/v1/compliance/cases/{id}`

**Response:**
```json
{
    "id": 500,
    "caseNumber": "CASE-2026-0001",
    "type": "SANCTIONS_MATCH",
    "status": "INVESTIGATING",
    "priority": "HIGH",
    "assignedTo": {
        "id": 10,
        "name": "John Analyst"
    },
    "merchant": { ... },
    "relatedAlerts": [ ... ],
    "timeline": [ ... ],
    "createdAt": "2026-01-08T14:00:00",
    "slaDeadline": "2026-01-15T14:00:00"
}
```

---

### 4.3 Get Case Timeline

**Endpoint:** `GET /api/v1/cases/{caseId}/timeline`

**Response:**
```json
[
    {
        "id": 1,
        "eventType": "CASE_CREATED",
        "description": "Case created from alert #100",
        "performedBy": "System",
        "timestamp": "2026-01-08T14:00:00"
    },
    {
        "id": 2,
        "eventType": "ASSIGNED",
        "description": "Assigned to John Analyst",
        "performedBy": "Admin User",
        "timestamp": "2026-01-08T14:30:00"
    }
]
```

---

### 4.4 Get Case Network Graph

**Endpoint:** `GET /api/v1/cases/{caseId}/network`

**Response:**
```json
{
    "nodes": [
        {"id": "merchant-1", "type": "MERCHANT", "label": "ABC Corp"},
        {"id": "pan-hash-1", "type": "PAN", "label": "****4242"}
    ],
    "edges": [
        {"source": "pan-hash-1", "target": "merchant-1", "type": "TRANSACTION"}
    ]
}
```

---

### 4.5 Get Case Statistics

Get statistics about compliance cases (open, in progress, total counts).

**Endpoint:** `GET /api/v1/compliance/cases/stats`

**Authentication:** Required (session-based)

**PSP Filtering:**
- Super Admin (PSP ID 0): Sees stats for all cases across all PSPs
- PSP users: See only their PSP's case statistics

**Response (200 OK):**
```json
{
    "openCases": 25,
    "inProgressCases": 15,
    "totalCases": 150
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated

---

### 4.6 Get Total Case Count

Get the total count of all compliance cases.

**Endpoint:** `GET /api/v1/compliance/cases/count`

**Authentication:** Required (session-based)

**PSP Filtering:**
- Super Admin (PSP ID 0): Sees count of all cases across all PSPs
- PSP users: See only their PSP's case count

**Response (200 OK):**
```json
{
    "count": 150
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated

---

### 4.7 Delete Case

Delete a compliance case by ID with PSP access validation.

**Endpoint:** `DELETE /api/v1/compliance/cases/{id}`

**Authentication:** Required (session-based)

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | Case ID to delete |

**PSP Security:**
- Super Admin: Can delete any case
- PSP users: Can only delete cases belonging to their PSP

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Case deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: User does not have permission to delete this case (PSP mismatch)
- `404 Not Found`: Case not found

---

## 6. Merchant APIs

### 5.1 List Merchants

**Endpoint:** `GET /api/v1/merchants`

**Response:**
```json
{
    "content": [
        {
            "id": 1,
            "merchantId": "MERCH-001",
            "businessName": "ABC Corporation",
            "mccCode": "5411",
            "riskLevel": "LOW",
            "kycStatus": "VERIFIED",
            "contractStatus": "ACTIVE",
            "dailyLimit": 50000000,
            "krs": 75.5,
            "cra": 60.0,
            "createdAt": "2025-06-01T00:00:00"
        }
    ]
}
```

---

### 5.2 Create Merchant

**Endpoint:** `POST /api/v1/merchants`

**Request Body:**
```json
{
    "merchantId": "MERCH-002",
    "businessName": "XYZ Store",
    "mccCode": "5812",
    "contactEmail": "contact@xyzstore.com",
    "contactPhone": "+1234567890",
    "dailyLimit": 10000000,
    "address": {
        "street": "123 Main St",
        "city": "Nairobi",
        "country": "KE"
    }
}
```

---

### 5.3 Onboard Merchant

Full merchant onboarding with KYC.

**Endpoint:** `POST /api/v1/merchants/onboard`

**Request Body:**
```json
{
    "businessName": "New Business Ltd",
    "registrationNumber": "REG123456",
    "mccCode": "5411",
    "contactDetails": { ... },
    "beneficialOwners": [
        {
            "name": "John Doe",
            "ownershipPercentage": 51,
            "idDocument": { ... }
        }
    ]
}
```

---

## 7. User Management APIs

### 6.1 List Users

**Endpoint:** `GET /api/v1/users`

**Response:**
```json
{
    "content": [
        {
            "id": 1,
            "email": "admin@aml.com",
            "firstName": "Super",
            "lastName": "Admin",
            "role": "SUPER_ADMIN",
            "active": true,
            "lastLogin": "2026-01-09T08:00:00"
        }
    ]
}
```

---

### 6.2 Create User

**Endpoint:** `POST /api/v1/users`

**Request Body:**
```json
{
    "email": "analyst@aml.com",
    "firstName": "New",
    "lastName": "Analyst",
    "password": "SecurePassword123!",
    "roleId": 3
}
```

---

### 6.3 Update User

**Endpoint:** `PUT /api/v1/users/{id}`

---

### 6.4 Get Current User

**Note:** The current user endpoint is documented in the [Authentication & Session Management APIs](#2-authentication--session-management-apis) section.

**Primary Endpoint:** `GET /api/v1/auth/me`

**Alternative Endpoint (if available):** `GET /api/v1/users/me`

See section [2.2 Get Current User](#22-get-current-user) for complete documentation.

---

### 6.5 Role Management

**List Roles:** `GET /api/v1/roles`

**Response:**
```json
[
    {
        "id": 1,
        "name": "ADMIN",
        "description": "Administrator role",
        "global": true,
        "permissions": ["VIEW_ALL", "MANAGE_USERS", "MANAGE_RULES"]
    }
]
```

---

## 8. Audit & Reporting APIs

### 7.1 Generate Audit Report

Generate a comprehensive audit report for a specified date range.

**Endpoint:** `POST /api/v1/audit/reports/generate`

**Authentication:** Required (session-based)

**Required Permissions:** `VIEW_AUDIT_LOGS` or `SUPER_ADMIN`

**Request Body:**
```json
{
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-15T23:59:59"
}
```

**Response (200 OK):**
```json
{
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-15T23:59:59",
    "generatedAt": "2026-01-16T09:00:00",
    "totalEvents": 15420,
    "uniqueUsers": 45,
    "uniqueEntityTypes": 8,
    "eventsByActionType": {
        "USER_LOGIN": 1250,
        "CASE_CREATED": 320,
        "CASE_UPDATED": 890,
        "ALERT_RESOLVED": 450,
        "MERCHANT_ONBOARDED": 75,
        "TRANSACTION_PROCESSED": 12435
    },
    "eventsByUser": {
        "admin@aml.com": 2340,
        "analyst1@aml.com": 1890,
        "analyst2@aml.com": 1650
    },
    "eventsByEntityType": {
        "MERCHANT": 3450,
        "TRANSACTION": 12435,
        "CASE": 1210,
        "ALERT": 890,
        "USER": 1250,
        "OTHER": 185
    }
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| startDate | DateTime | Start of the audit period |
| endDate | DateTime | End of the audit period |
| generatedAt | DateTime | Timestamp when the report was generated |
| totalEvents | Long | Total number of audit events in the period |
| uniqueUsers | Integer | Number of unique users who performed actions |
| uniqueEntityTypes | Integer | Number of different entity types involved |
| eventsByActionType | Map | Breakdown of events by action type |
| eventsByUser | Map | Breakdown of events by user |
| eventsByEntityType | Map | Breakdown of events by entity type |

**Error Responses:**
- `400 Bad Request`: Invalid date range (end date before start date)
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: Insufficient permissions

**Notes:**
- Reports are generated in real-time from the audit trail database
- Large date ranges may take longer to process
- All timestamps are in ISO 8601 format
- PSP filtering is automatically applied based on user role

---

### 7.2 Generate User Activity Report

Generate a detailed activity report for a specific user.

**Endpoint:** `POST /api/v1/audit/reports/user-activity`

**Authentication:** Required (session-based)

**Required Permissions:** `VIEW_AUDIT_LOGS` or `SUPER_ADMIN`

**Request Body:**
```json
{
    "userId": "analyst1@aml.com",
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-15T23:59:59"
}
```

**Response (200 OK):**
```json
{
    "userId": "analyst1@aml.com",
    "startDate": "2026-01-01T00:00:00",
    "endDate": "2026-01-15T23:59:59",
    "totalActions": 1890,
    "actionsByType": {
        "CASE_CREATED": 45,
        "CASE_UPDATED": 230,
        "ALERT_RESOLVED": 125,
        "CASE_ASSIGNED": 78,
        "CASE_ESCALATED": 12,
        "COMMENT_ADDED": 340,
        "DOCUMENT_UPLOADED": 89,
        "USER_LOGIN": 95,
        "REPORT_GENERATED": 23,
        "OTHER": 853
    }
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| userId | String | User identifier (email or username) |
| startDate | DateTime | Start of the activity period |
| endDate | DateTime | End of the activity period |
| totalActions | Long | Total number of actions performed by the user |
| actionsByType | Map | Breakdown of actions by type |

**Error Responses:**
- `400 Bad Request`: Invalid date range or user ID not found
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: Insufficient permissions or attempting to view another PSP's user activity

**Notes:**
- PSP users can only generate reports for users within their own PSP
- Super Admin can generate reports for any user
- Action types are dynamically determined from the audit trail

---

### 7.3 Get Audit Trail

Retrieve paginated audit trail entries with filtering.

**Endpoint:** `GET /api/v1/audit/trail`

**Authentication:** Required (session-based)

**Required Permissions:** `VIEW_AUDIT_LOGS` or `SUPER_ADMIN`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| page | Integer | Page number (default: 0) |
| size | Integer | Page size (default: 50, max: 200) |
| startDate | DateTime | Filter by start date (ISO 8601) |
| endDate | DateTime | Filter by end date (ISO 8601) |
| userId | String | Filter by user ID |
| action | String | Filter by action type |
| entityType | String | Filter by entity type (MERCHANT, CASE, ALERT, etc.) |
| merchantId | Long | Filter by merchant ID |

**PSP Filtering:**
- Super Admin (PSP ID 0): Sees all audit trail entries across all PSPs
- PSP users: See only audit trail entries for their PSP

**Response (200 OK):**
```json
{
    "content": [
        {
            "id": 12345,
            "action": "CASE_CREATED",
            "performedBy": "analyst1@aml.com",
            "performedAt": "2026-01-15T14:30:00",
            "merchantId": 42,
            "details": "Created case CASE-2026-0123 for high-risk transaction",
            "ipAddress": "192.168.1.100",
            "pspId": 5
        }
    ],
    "totalElements": 15420,
    "totalPages": 309,
    "number": 0,
    "size": 50
}
```

**Error Responses:**
- `400 Bad Request`: Invalid query parameters
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: Insufficient permissions

---

## 9. Analytics APIs

### 8.1 Risk Heatmap - Customer

**Endpoint:** `GET /api/v1/analytics/risk/heatmap/customer`

**Response:**
```json
{
    "customer1": 25,
    "customer2": 65,
    "customer3": 45
}
```

---

### 7.2 Risk Heatmap - Merchant

**Endpoint:** `GET /api/v1/analytics/risk/heatmap/merchant`

**Response:**
```json
{
    "merchant1": 30,
    "merchant2": 70,
    "merchant3": 50
}
```

---

### 7.3 Risk Heatmap - Geographic

**Endpoint:** `GET /api/v1/analytics/risk/heatmap/geographic`

**Response:**
```json
{
    "countries": [
        {"code": "KE", "name": "Kenya", "riskScore": 25, "transactionCount": 5000},
        {"code": "NG", "name": "Nigeria", "riskScore": 65, "transactionCount": 2000}
    ]
}
```

---

### 7.4 Risk Trends

**Endpoint:** `GET /api/v1/analytics/risk/trends`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| days | int | Number of days (default: 30) |
| startDate | String | Start date (optional) |
| endDate | String | End date (optional) |
| granularity | String | DAILY, WEEKLY, MONTHLY (optional) |

**Response:**
```json
{
    "labels": ["2026-01-01", "2026-01-02", "2026-01-03"],
    "data": [25, 30, 28]
}
```

---

## 9. Reporting APIs

### 8.1 SAR Reports

**List SARs:** `GET /api/v1/compliance/sar`

**Create SAR:** `POST /api/v1/compliance/sar`

**Get SAR Details:** `GET /api/v1/compliance/sar/{id}`

**Submit SAR:** `POST /api/v1/compliance/sar/{id}/file`

**Delete SAR:** `DELETE /api/v1/compliance/sar/{id}`

---

### 8.2 Audit Logs

**Endpoint:** `GET /api/v1/audit/logs`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| limit | int | Maximum number of logs to return (default: 100, max: 1000) |
| username | String | Filter by username |
| actionType | String | Filter by action type |
| entityType | String | Filter by entity type |
| entityId | String | Filter by entity ID |
| success | Boolean | Filter by success status |
| ipAddress | String | Filter by IP address |
| sessionId | String | Filter by session ID |
| start | String | Start date (ISO format) |
| end | String | End date (ISO format) |

**Response (200 OK):**
```json
[
    {
        "id": 1,
        "username": "admin",
        "actionType": "CREATE",
        "entityType": "MERCHANT",
        "entityId": "123",
        "timestamp": "2026-01-09T10:00:00",
        "success": true,
        "ipAddress": "192.168.1.1",
        "sessionId": "abc123"
    }
]
```

---

## 10. Sanctions Screening APIs

### 9.1 Screen Entity

**Endpoint:** `POST /api/v1/sanctions/screen`

**Request Body:**
```json
{
    "name": "John Doe",
    "type": "INDIVIDUAL",
    "country": "US",
    "dateOfBirth": "1980-01-15"
}
```

**Response:**
```json
{
    "screeningId": "SCR-12345",
    "matchStatus": "POTENTIAL_MATCH",
    "matchScore": 0.85,
    "matches": [
        {
            "source": "OFAC_SDN",
            "name": "JOHN DOE",
            "score": 0.85,
            "listingDate": "2020-01-01"
        }
    ]
}
```

---

### 9.2 Batch Screening

**Endpoint:** `POST /api/v1/sanctions/screen/batch`

---

## 11. Dashboard APIs

### 10.1 Dashboard Statistics

**Endpoint:** `GET /api/v1/dashboard/stats`

**Response:**
```json
{
    "totalMerchants": 150,
    "activeMerchants": 120,
    "pendingScreening": 5,
    "openCases": 25,
    "urgentCases": 3
}
```

---

### 10.2 Transaction Volume

**Endpoint:** `GET /api/v1/dashboard/transaction-volume`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| days | int | Number of days (default: 7) |

**Response:**
```json
{
    "labels": ["Jan 1", "Jan 2", "Jan 3"],
    "data": [1500, 1800, 1650],
    "pspId": 1
}
```

---

### 10.3 Risk Distribution

**Endpoint:** `GET /api/v1/dashboard/risk-distribution`

**Response:**
```json
{
    "LOW": 150,
    "MEDIUM": 45,
    "HIGH": 12
}
```

---

### 10.4 Live Alerts

**Endpoint:** `GET /api/v1/dashboard/live-alerts`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| limit | int | Maximum number of alerts (default: 5) |

**Response:**
```json
[
    {
        "id": 100,
        "type": "HIGH_FRAUD_SCORE",
        "priority": "HIGH",
        "status": "OPEN",
        "description": "Fraud score exceeded threshold",
        "createdAt": "2026-01-09T10:35:00"
    }
]
```

---

### 10.5 Recent Transactions

**Endpoint:** `GET /api/v1/dashboard/recent-transactions`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| limit | int | Maximum number of transactions (default: 5) |

**Response:**
```json
[
    {
        "id": 12345,
        "merchantId": "MERCH-001",
        "amountCents": 10000,
        "currency": "USD",
        "decision": "ALLOW",
        "txnTs": "2026-01-09T10:30:00"
    }
]
```

---

### 10.6 Reporting Summary

**Endpoint:** `GET /api/v1/reporting/summary`

**Response:**
```json
{
    "casesByStatus": {
        "NEW": 10,
        "ASSIGNED": 5,
        "INVESTIGATING": 8,
        "RESOLVED": 50
    },
    "sarsByStatus": {
        "DRAFT": 2,
        "FILED": 15
    },
    "auditLast24h": 150
}
```

---

## 12. Health & Monitoring APIs

### 11.1 Health Check

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
    "status": "UP",
    "components": {
        "db": {"status": "UP"},
        "redis": {"status": "UP"},
        "aerospike": {"status": "UP"}
    }
}
```

---

### 11.2 Prometheus Metrics

**Endpoint:** `GET /actuator/prometheus`

---

## 13. Compliance Calendar APIs

### 12.1 Upcoming Deadlines

**Endpoint:** `GET /api/v1/compliance/calendar/upcoming`

**Response:**
```json
[
    {
        "id": 1,
        "deadlineType": "SAR_FILING",
        "deadlineDate": "2026-01-15",
        "caseId": 100,
        "description": "SAR filing deadline for Case #100"
    }
]
```

---

### 12.2 Overdue Deadlines

**Endpoint:** `GET /api/v1/compliance/calendar/overdue`

**Response:**
```json
[
    {
        "id": 2,
        "deadlineType": "SAR_FILING",
        "deadlineDate": "2026-01-05",
        "caseId": 99,
        "description": "Overdue SAR filing for Case #99",
        "daysOverdue": 4
    }
]
```

---

## 14. Regulatory Reporting APIs

### 13.1 Generate Regulatory Report

**Endpoint:** `GET /api/v1/reporting/regulatory/{type}`

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| type | String | Report type: `ctr`, `lctr`, or `iftr` |

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | String | Start date (ISO format, optional) |
| endDate | String | End date (ISO format, optional) |

**Response (CTR):**
```json
{
    "reportType": "CTR",
    "period": {
        "startDate": "2026-01-01",
        "endDate": "2026-01-31"
    },
    "totalTransactions": 150,
    "totalAmount": 1500000.00,
    "currency": "USD",
    "transactions": [...]
}
```

---

## 15. Transaction Monitoring APIs

### 14.1 Monitoring Dashboard Stats

**Endpoint:** `GET /api/v1/monitoring/dashboard/stats`

**Response:**
```json
{
    "totalMonitored": 5000,
    "blockedCount": 50,
    "heldCount": 120,
    "alertCount": 200,
    "allowedCount": 4630
}
```

---

### 14.2 Risk Distribution

**Endpoint:** `GET /api/v1/monitoring/risk-distribution`

**Response:**
```json
{
    "LOW": 3000,
    "MEDIUM": 1500,
    "HIGH": 400,
    "CRITICAL": 100
}
```

---

### 14.3 Risk Indicators

**Endpoint:** `GET /api/v1/monitoring/risk-indicators`

**Response:**
```json
[
    {
        "indicator": "High Volume Spike",
        "count": 25,
        "riskLevel": "HIGH"
    },
    {
        "indicator": "Unusual Geographic Pattern",
        "count": 15,
        "riskLevel": "MEDIUM"
    }
]
```

---

### 14.4 Recent Activity

**Endpoint:** `GET /api/v1/monitoring/recent-activity`

**Response:**
```json
[
    {
        "timestamp": "2026-01-09T10:30:00",
        "action": "Transaction Blocked",
        "description": "Transaction #12345 blocked due to high risk score",
        "transactionId": 12345
    }
]
```

---

### 14.5 Monitored Transactions

**Endpoint:** `GET /api/v1/monitoring/transactions`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| riskLevel | String | Filter by risk level (optional) |
| decision | String | Filter by decision: BLOCK, HOLD, ALERT, ALLOW (optional) |
| limit | int | Maximum number of transactions (default: 50) |

**Response:**
```json
[
    {
        "id": 12345,
        "merchantId": "MERCH-001",
        "amountCents": 10000,
        "decision": "BLOCK",
        "riskScore": 0.95,
        "txnTs": "2026-01-09T10:30:00"
    }
]
```

---

## 16. Client Registration APIs

### 15.1 Register Client

**Endpoint:** `POST /api/v1/clients/register`

**Request Body:**
```json
{
    "clientName": "Merchant ABC",
    "contactEmail": "contact@merchantabc.com",
    "contactPhone": "+1234567890",
    "description": "Retail merchant"
}
```

**Response:**
```json
{
    "clientId": 1,
    "clientName": "Merchant ABC",
    "apiKey": "abc123xyz789...",
    "status": "ACTIVE",
    "createdAt": "2026-01-09T10:00:00"
}
```

---

## 17. Batch Processing APIs

### 16.1 Trigger Batch Scoring

**Endpoint:** `POST /api/v1/batch/score/yesterday`

### 16.2 Backfill Features

**Endpoint:** `POST /api/v1/batch/backfill/features`

---

## 18. Grafana Integration APIs

### 18.1 Get User Context

**Endpoint:** `GET /api/v1/grafana/user-context`

**Description:** Returns the authenticated user's PSP code and role for Grafana dashboard filtering.

**Authentication:** Required (Session-based)

**Response (200 OK):**
```json
{
    "user_role": "PSP_USER",
    "psp_code": "PSP_M-PESA",
    "psp_id": "1",
    "can_view_all_psps": "false"
}
```

**Response for Platform Administrator:**
```json
{
    "user_role": "PLATFORM_ADMIN",
    "psp_code": "ALL",
    "psp_id": "ALL",
    "can_view_all_psps": "true"
}
```

**Note:** This endpoint is used by Grafana dashboards to automatically filter data based on user role. PSP users see only their PSP's data, while Platform Administrators can view all PSPs.

---

### 18.2 Get Available Dashboards

**Endpoint:** `GET /api/v1/grafana/dashboards`

**Description:** Returns a list of available Grafana dashboards filtered by user role. Follows the recommended pattern of backend-driven dashboard menu (not hardcoded in frontend).

**Authentication:** Required (Session-based)

**Response (200 OK):**
```json
[
    {
        "menu": "TRANSACTION OVERVIEW",
        "uid": "transaction-overview",
        "path": "/dashboard/transactions",
        "systemOnly": false
    },
    {
        "menu": "AML RISK",
        "uid": "aml-risk",
        "path": "/dashboard/aml-risk",
        "systemOnly": false
    },
    {
        "menu": "FRAUD DETECTION",
        "uid": "fraud-detection",
        "path": "/dashboard/fraud",
        "systemOnly": false
    },
    {
        "menu": "COMPLIANCE",
        "uid": "compliance",
        "path": "/dashboard/compliance",
        "systemOnly": false
    },
    {
        "menu": "MODEL PERFORMANCE",
        "uid": "model-performance",
        "path": "/dashboard/models",
        "systemOnly": false
    },
    {
        "menu": "SCREENING",
        "uid": "screening",
        "path": "/dashboard/screening",
        "systemOnly": false
    },
    {
        "menu": "SYSTEM PERFORMANCE",
        "uid": "system-performance",
        "path": "/dashboard/system",
        "systemOnly": true
    },
    {
        "menu": "INFRASTRUCTURE",
        "uid": "infrastructure-resources",
        "path": "/dashboard/infrastructure",
        "systemOnly": true
    },
    {
        "menu": "THREAD POOLS",
        "uid": "thread-pools-throughput",
        "path": "/dashboard/threads",
        "systemOnly": true
    },
    {
        "menu": "CIRCUIT BREAKER",
        "uid": "circuit-breaker-resilience",
        "path": "/dashboard/circuit-breaker",
        "systemOnly": true
    }
]
```

**Response Fields:**
- `menu`: Display name for the dashboard menu item
- `uid`: Grafana dashboard UID (used in iframe URL: `/d/{uid}`)
- `path`: Frontend route path for the dashboard
- `systemOnly`: `true` if dashboard is only available to system administrators, `false` otherwise

**Role-Based Filtering:**
- **PSP Users**: Receive only dashboards with `systemOnly: false`
- **System Administrators**: Receive all dashboards (both `systemOnly: true` and `false`)

**Note:** This endpoint enables dynamic dashboard menu generation based on user permissions, following production-grade integration patterns. The frontend should use this endpoint to build the dashboard navigation menu instead of hardcoding dashboard lists.

---

## 19. PSP Data Isolation

### 18.1 PSP Filtering

All endpoints that return PSP-scoped data automatically filter results based on the authenticated user's role:

- **PSP Users** (`PSP_ADMIN`, `PSP_ANALYST`, `PSP_USER`): Automatically filtered to their PSP
- **Platform Administrators** (`ADMIN`, `MLRO`, `PLATFORM_ADMIN`): Can access all PSPs

### 18.2 PSP ID Parameter

For endpoints that accept `pspId` as a query parameter:

- **PSP Users**: The `pspId` parameter is automatically sanitized - they cannot override their PSP ID
- **Platform Administrators**: Can specify any `pspId` or omit it to view all PSPs

**Example:**
```bash
# PSP User - automatically filtered to their PSP (pspId parameter ignored)
GET /api/v1/transactions?pspId=999
# Returns: Only transactions from user's PSP (not PSP 999)

# Platform Admin - can filter by PSP
GET /api/v1/transactions?pspId=1
# Returns: Transactions from PSP 1

# Platform Admin - view all PSPs
GET /api/v1/transactions
# Returns: All transactions from all PSPs
```

### 18.3 Security

- PSP users attempting to access another PSP's data will receive `403 Forbidden`
- Security violations are logged for audit purposes
- See `PSP_ISOLATION_SECURITY_AUDIT.md` for detailed security documentation

---

## Appendix: Complete Endpoint List

| Method | Endpoint | Description | PSP Filtering |
|--------|----------|-------------|----------------|
| POST | /api/v1/transactions/ingest | Ingest transaction | N/A |
| GET | /api/v1/transactions | List transactions | ✅ Auto-filtered |
| GET | /api/v1/transactions/{id} | Get transaction | ✅ Validated |
| GET | /api/v1/alerts | List alerts | ✅ Auto-filtered |
| GET | /api/v1/alerts/{id} | Get alert | ✅ Validated |
| PUT | /api/v1/alerts/{id}/resolve | Resolve alert | ✅ Validated |
| GET | /api/v1/compliance/cases | List cases | ✅ Auto-filtered |
| GET | /api/v1/compliance/cases/{id} | Get case | ✅ Validated |
| GET | /api/v1/cases/{id}/timeline | Case timeline | ✅ Validated |
| GET | /api/v1/merchants | List merchants | ✅ Auto-filtered |
| POST | /api/v1/merchants | Create merchant | N/A |
| GET | /api/v1/merchants/{id} | Get merchant | ✅ Validated |
| GET | /api/v1/users | List users | ✅ Auto-filtered |
| POST | /api/v1/users | Create user | ✅ Validated |
| GET | /api/v1/users/{id} | Get user | ✅ Validated |
| POST | /api/v1/sanctions/screen | Screen entity | N/A |
| GET | /api/v1/audit/logs | Get audit logs | ✅ Auto-filtered |
| GET | /api/v1/compliance/sar | List SAR reports | ✅ Auto-filtered |
| GET | /api/v1/compliance/calendar/upcoming | Upcoming deadlines | ✅ Auto-filtered |
| GET | /api/v1/compliance/calendar/overdue | Overdue deadlines | ✅ Auto-filtered |
| GET | /api/v1/dashboard/stats | Dashboard statistics | ✅ Auto-filtered |
| GET | /api/v1/dashboard/transaction-volume | Transaction volume | ✅ Auto-filtered |
| GET | /api/v1/dashboard/risk-distribution | Risk distribution | ✅ Auto-filtered |
| GET | /api/v1/dashboard/live-alerts | Live alerts | ✅ Auto-filtered |
| GET | /api/v1/dashboard/recent-transactions | Recent transactions | ✅ Auto-filtered |
| GET | /api/v1/reporting/summary | Reporting summary | ✅ Auto-filtered |
| GET | /api/v1/reporting/regulatory/{type} | Regulatory reports | ✅ Auto-filtered |
| GET | /api/v1/monitoring/dashboard/stats | Monitoring stats | ✅ Auto-filtered |
| GET | /api/v1/monitoring/risk-distribution | Monitoring risk distribution | ✅ Auto-filtered |
| GET | /api/v1/monitoring/risk-indicators | Risk indicators | ✅ Auto-filtered |
| GET | /api/v1/monitoring/recent-activity | Recent activity | ✅ Auto-filtered |
| GET | /api/v1/monitoring/transactions | Monitored transactions | ✅ Auto-filtered |
| GET | /api/v1/limits/velocity-rules | List velocity rules | ✅ Auto-filtered |
| POST | /api/v1/limits/velocity-rules | Create velocity rule | ✅ Validated |
| PUT | /api/v1/limits/velocity-rules/{id} | Update velocity rule | ✅ Validated |
| DELETE | /api/v1/limits/velocity-rules/{id} | Delete velocity rule | ✅ Validated |
| GET | /api/v1/limits/risk-thresholds | List risk thresholds | ✅ Auto-filtered |
| POST | /api/v1/limits/risk-thresholds | Create/update risk threshold | ✅ Validated |
| GET | /api/v1/psps | List PSPs | ✅ Auto-filtered |
| GET | /api/v1/psps/{id} | Get PSP by ID | ✅ Validated |
| GET | /api/v1/users/me | Get current user | ✅ Auto-filtered |
| GET | /api/v1/roles | List roles | ✅ Auto-filtered |
| GET | /api/v1/grafana/user-context | Get Grafana user context | N/A |
| GET | /api/v1/grafana/dashboards | Get available Grafana dashboards | Role-based |
| GET | /actuator/health | Health check |
| POST | /api/v1/rules/generate | Generate Rules with AI |
| GET | /api/v1/rules | List Dynamic Rules |
| GET | /api/v1/rules/{id} | Get single rule | ✅ Validated |
| POST | /api/v1/rules | Create Rule |
| PUT | /api/v1/rules/{id} | Update Rule | ✅ Validated |
| DELETE | /api/v1/rules/{id} | Delete Rule |
| POST | /api/v1/rules/{id}/enable | Enable rule | ✅ Validated |
| POST | /api/v1/rules/{id}/disable | Disable rule | ✅ Validated |
| GET | /api/v1/rules/{id}/effectiveness | Rule effectiveness | ✅ Validated |

---

## 20. Rules Generation APIs

### 19.1 Generate Rules (AI)

**Endpoint:** `POST /api/v1/rules/generate`

**Request Body:**
```json
{
    "prompt": "Create a rule to flag transactions over $10,000 from high risk countries",
    "riskLevel": "HIGH",
    "ruleType": "TRANSACTION"
}
```

**Response:**
```json
{
    "ruleId": "GENERATED_RULE_123",
    "ruleContent": "rule \"High Value High Risk\" when ... then ... end",
    "description": "Flags transactions > 10000 from high risk countries",
    "status": "DRAFT"
}
```

### 19.2 List Rules

**Endpoint:** `GET /api/v1/rules`

**Response:**
```json
[
    {
        "id": 1,
        "name": "Block High Risk Country",
        "description": "Blocks transactions from high-risk countries",
        "ruleType": "DROOLS_DRL",
        "ruleExpression": "rule \"Block High Risk\" when ... then ... end",
        "priority": 100,
        "enabled": true,
        "pspId": null,
        "createdBy": 1,
        "createdAt": "2026-01-09T08:00:00"
    }
]
```

---

### 19.3 Get Single Rule

**Endpoint:** `GET /api/v1/rules/{id}`

**Response:**
```json
{
    "id": 1,
    "name": "Block High Risk Country",
    "description": "Blocks transactions from high-risk countries",
    "ruleType": "DROOLS_DRL",
    "ruleExpression": "rule \"Block High Risk\" when ... then ... end",
    "priority": 100,
    "enabled": true,
    "pspId": null,
    "createdBy": 1,
    "createdAt": "2026-01-09T08:00:00"
}
```

---

### 19.4 Create Rule

**Endpoint:** `POST /api/v1/rules`

**Request Body:**
```json
{
    "name": "New Rule",
    "description": "Manual rule",
    "ruleType": "SPEL",
    "ruleExpression": "#tx.amount >= 10000",
    "priority": 100,
    "enabled": true,
    "action": "ALERT"
}
```

**Note:** `pspId` is automatically set based on the current user. Super admin rules have `pspId: null`.

---

### 19.5 Update Rule

**Endpoint:** `PUT /api/v1/rules/{id}`

**Request Body:**
```json
{
    "name": "Updated Rule",
    "description": "Updated description",
    "ruleExpression": "#tx.amount >= 15000",
    "priority": 90,
    "enabled": true
}
```

**Note:** Only super admin can update super admin rules (`pspId: null`). PSP users can only update their own PSP's rules.

---

### 19.6 Enable Rule

**Endpoint:** `POST /api/v1/rules/{id}/enable`

**Response:**
```json
{
    "id": 1,
    "enabled": true,
    "message": "Rule enabled successfully"
}
```

---

### 19.7 Disable Rule

**Endpoint:** `POST /api/v1/rules/{id}/disable`

**Response:**
```json
{
    "id": 1,
    "enabled": false,
    "message": "Rule disabled successfully"
}
```

---

### 19.8 Get Rule Effectiveness

**Endpoint:** `GET /api/v1/rules/{id}/effectiveness`

**Response:**
```json
{
    "ruleId": 1,
    "ruleName": "Block High Risk Country",
    "totalExecutions": 10000,
    "triggeredCount": 250,
    "falsePositiveRate": 0.15,
    "truePositiveRate": 0.85,
    "averageExecutionTime": 2.5,
    "lastExecutedAt": "2026-01-09T10:30:00"
}
```

---

### 19.9 Delete Rule

**Endpoint:** `DELETE /api/v1/rules/{id}`

**Response:** `204 No Content`

**Note:** Only super admin can delete super admin rules. PSP users can only delete their own PSP's rules.

---

## 20. Settings Management APIs

### 20.1 Get System Settings

Get current system configuration settings.

**Endpoint:** `GET /api/v1/settings/system`

**Authentication:** Required (ADMIN role only)

**Response (200 OK):**
```json
{
    "maintenanceMode": false,
    "debugLogging": false,
    "riskThresholdHigh": 80,
    "riskThresholdMedium": 50,
    "auditRetentionDays": 90,
    "allowCrossBorderTxns": true
}
```

---

### 20.2 Update System Settings

Update system configuration settings (Super Admin only).

**Endpoint:** `PUT /api/v1/settings/system`

**Authentication:** Required (ADMIN role only)

**Request Body:**
```json
{
    "maintenanceMode": false,
    "debugLogging": false,
    "riskThresholdHigh": 80,
    "riskThresholdMedium": 50,
    "auditRetentionDays": 90,
    "allowCrossBorderTxns": true
}
```

**Note:** All fields are optional. Only provided fields will be updated.

**Response (200 OK):**
```json
{
    "maintenanceMode": false,
    "debugLogging": false,
    "riskThresholdHigh": 80,
    "riskThresholdMedium": 50,
    "auditRetentionDays": 90,
    "allowCrossBorderTxns": true
}
```

---

### 20.3 Get All PSPs

Get list of all PSPs for theme management (Super Admin only).

**Endpoint:** `GET /api/v1/settings/psps`

**Authentication:** Required (ADMIN role only)

**Response (200 OK):**
```json
[
    {
        "id": 1,
        "code": "PSP001",
        "name": "Payment Service Provider 1",
        "status": "ACTIVE"
    },
    {
        "id": 2,
        "code": "PSP002",
        "name": "Payment Service Provider 2",
        "status": "ACTIVE"
    }
]
```

---

### 20.4 Get PSP Theme

Get theme configuration for a specific PSP.

**Endpoint:** `GET /api/v1/settings/psps/{pspId}/theme`

**Authentication:** Required (ADMIN role only)

**Path Parameters:**
- `pspId` (Long, required): PSP ID

**Response (200 OK):**
```json
{
    "pspId": 1,
    "pspName": "Payment Service Provider 1",
    "brandingTheme": "default",
    "primaryColor": "#8B4049",
    "secondaryColor": "#C9A961",
    "accentColor": "#A0525C",
    "logoUrl": "https://example.com/logo.png",
    "fontFamily": "'Inter', 'Outfit', sans-serif",
    "fontSize": "14px",
    "buttonRadius": "12px",
    "buttonStyle": "flat",
    "navStyle": "drawer"
}
```

---

### 20.5 Update PSP Theme

Update theme configuration for a specific PSP.

**Endpoint:** `PUT /api/v1/settings/psps/{pspId}/theme`

**Authentication:** Required (ADMIN role only)

**Path Parameters:**
- `pspId` (Long, required): PSP ID

**Request Body:**
```json
{
    "brandingTheme": "default",
    "primaryColor": "#8B4049",
    "secondaryColor": "#C9A961",
    "accentColor": "#A0525C",
    "logoUrl": "https://example.com/logo.png",
    "faviconUrl": "https://example.com/favicon.ico",
    "fontFamily": "'Inter', 'Outfit', sans-serif",
    "fontSize": "14px",
    "buttonRadius": "12px",
    "buttonStyle": "flat",
    "navStyle": "drawer",
    "customCss": ".custom-class { color: red; }"
}
```

**Note:** All fields are optional. Only provided fields will be updated.

**Response (200 OK):**
```json
{
    "pspId": 1,
    "pspName": "Payment Service Provider 1",
    "brandingTheme": "default",
    "primaryColor": "#8B4049",
    "secondaryColor": "#C9A961",
    "accentColor": "#A0525C",
    "logoUrl": "https://example.com/logo.png",
    "fontFamily": "'Inter', 'Outfit', sans-serif",
    "fontSize": "14px",
    "buttonRadius": "12px",
    "buttonStyle": "flat",
    "navStyle": "drawer"
}
```

---

### 20.6 Get Theme Presets

Get available theme color presets.

**Endpoint:** `GET /api/v1/settings/themes/presets`

**Authentication:** Required (ADMIN role only)

**Response (200 OK):**
```json
{
    "default": {
        "primaryColor": "#a93226",
        "secondaryColor": "#922b21",
        "accentColor": "#8B4049"
    },
    "burgundy": {
        "primaryColor": "#800020",
        "secondaryColor": "#9B2D30",
        "accentColor": "#A52A2A"
    },
    "emerald": {
        "primaryColor": "#50C878",
        "secondaryColor": "#00A86B",
        "accentColor": "#028A0F"
    },
    "purple": {
        "primaryColor": "#6A0DAD",
        "secondaryColor": "#8B00FF",
        "accentColor": "#9370DB"
    }
}
```

---

