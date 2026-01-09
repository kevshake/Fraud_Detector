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
- **Authentication**: Session-based (Form Login)
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

## 2. Transaction APIs

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
        "4F": "A0000000041010"
    }
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
    "latencyMs": 45
}
```

| Field | Type | Description |
|-------|------|-------------|
| txnId | Long | Assigned transaction ID |
| score | Double | Fraud score (0.0 - 1.0) |
| action | String | BLOCK, HOLD, ALERT, or ALLOW |
| reasons | Array | List of triggered rules |
| latencyMs | Long | Processing time in ms |

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
    "createdAt": "2026-01-09T10:30:00"
}
```

---

## 3. Alert APIs

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

### 3.4 Alert Disposition Statistics

**Endpoint:** `GET /api/v1/alerts/disposition-stats`

**Response:**
```json
{
    "totalAlerts": 1000,
    "falsePositives": 650,
    "confirmedFraud": 150,
    "escalated": 100,
    "pending": 100,
    "falsePositiveRate": 0.65
}
```

---

## 4. Case Management APIs

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

## 5. Merchant APIs

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

## 6. User Management APIs

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

## 7. Analytics APIs

### 7.1 Risk Heatmap - Customer

**Endpoint:** `GET /api/v1/analytics/risk/heatmap/customer`

---

### 7.2 Risk Heatmap - Geographic

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

### 7.3 Risk Trends

**Endpoint:** `GET /api/v1/analytics/risk/trends`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | String | Start date |
| endDate | String | End date |
| granularity | String | DAILY, WEEKLY, MONTHLY |

---

## 8. Reporting APIs

### 8.1 SAR Reports

**List SARs:** `GET /api/v1/compliance/sars`

**Create SAR:** `POST /api/v1/compliance/sars`

**Get SAR Details:** `GET /api/v1/compliance/sars/{id}`

**Submit SAR:** `POST /api/v1/compliance/sars/{id}/submit`

---

### 8.2 Audit Logs

**Endpoint:** `GET /api/v1/audit-logs`

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| userId | Long | Filter by user |
| action | String | Filter by action type |
| startDate | String | Start date |
| endDate | String | End date |

---

## 9. Sanctions Screening APIs

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

## 10. Health & Monitoring APIs

### 10.1 Health Check

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

### 10.2 Prometheus Metrics

**Endpoint:** `GET /actuator/prometheus`

---

## 11. Client Registration APIs

### 11.1 Register Client

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

## 12. Batch Processing APIs

### 12.1 Trigger Batch Scoring

**Endpoint:** `POST /api/v1/batch/score/yesterday`

### 12.2 Backfill Features

**Endpoint:** `POST /api/v1/batch/backfill/features`

---

## 13. Grafana Integration APIs

### 13.1 Get User Context

**Endpoint:** `GET /api/v1/grafana/user-context`

**Description:** Returns the authenticated user's PSP code and role for Grafana dashboard filtering.

**Authentication:** Required (Session-based)

**Response (200 OK):**
```json
{
    "username": "psp_admin_1",
    "userRole": "PSP_ADMIN",
    "pspCode": "PSP_M-PESA",
    "psp_id": "1",
    "can_view_all_psps": "false"
}
```

**Response for Platform Administrator:**
```json
{
    "username": "platform_admin",
    "userRole": "ADMIN",
    "psp_code": "ALL",
    "psp_id": "ALL",
    "can_view_all_psps": "true"
}
```

**Note:** This endpoint is used by Grafana dashboards to automatically filter data based on user role. PSP users see only their PSP's data, while Platform Administrators can view all PSPs.

---

## 14. PSP Data Isolation

### 14.1 PSP Filtering

All endpoints that return PSP-scoped data automatically filter results based on the authenticated user's role:

- **PSP Users** (`PSP_ADMIN`, `PSP_ANALYST`, `PSP_USER`): Automatically filtered to their PSP
- **Platform Administrators** (`ADMIN`, `MLRO`, `PLATFORM_ADMIN`): Can access all PSPs

### 14.2 PSP ID Parameter

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

### 14.3 Security

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
| GET | /api/v1/audit-logs | Get audit logs | ✅ Auto-filtered |
| GET | /api/v1/grafana/user-context | Get Grafana user context | N/A |
| GET | /actuator/health | Health check |
