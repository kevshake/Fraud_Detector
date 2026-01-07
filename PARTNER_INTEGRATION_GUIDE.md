# AML Fraud Detector - Partner Integration Guide

**Version:** 1.0  
**Last Updated:** January 2026  
**Base URL:** `https://api.your-domain.com/api/v1`  
**Default Port:** `2637`

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication](#authentication)
3. [Transaction Validation & Screening](#transaction-validation--screening)
4. [Merchant Onboarding](#merchant-onboarding)
5. [Sanctions Screening](#sanctions-screening)
6. [Batch Operations](#batch-operations)
7. [Monitoring & Reporting](#monitoring--reporting)
8. [Pricing & Billing](#pricing--billing)
9. [Error Handling](#error-handling)
10. [Rate Limits & Best Practices](#rate-limits--best-practices)
11. [Code Examples](#code-examples)
12. [Support & Contact](#support--contact)

---

## Overview

The AML Fraud Detector provides comprehensive anti-money laundering (AML) and fraud detection services for payment service providers (PSPs), merchants, and financial institutions. This guide covers all available APIs for partner integration.

### Key Features

- **Real-time Transaction Screening:** Validate transactions against fraud patterns and AML rules
- **Sanctions Screening:** Screen entities against international sanctions lists
- **Merchant Onboarding:** Automated merchant risk assessment and KYC
- **Batch Processing:** High-throughput batch screening capabilities
- **Compliance Reporting:** Generate regulatory reports (SAR, CTR, etc.)
- **Risk Analytics:** Real-time risk scoring and monitoring

### Service Architecture

- **Base Path:** `/api/v1`
- **Protocol:** HTTPS (TLS 1.2+)
- **Content-Type:** `application/json`
- **Response Format:** JSON

---

## Authentication

### Client Registration

Before using the API, partners must register and obtain API credentials.

#### Register New Client

**Endpoint:** `POST /api/v1/clients/register`

**Request:**
```json
{
  "clientName": "Your Company Name",
  "contactEmail": "contact@yourcompany.com",
  "contactPhone": "+1234567890",
  "description": "Payment service provider processing card transactions"
}
```

**Response:** `201 Created`
```json
{
  "clientId": 1,
  "clientName": "Your Company Name",
  "apiKey": "abc123xyz789def456ghi012jkl345mno678pqr901stu234vwx567yz",
  "contactEmail": "contact@yourcompany.com",
  "contactPhone": "+1234567890",
  "status": "ACTIVE",
  "createdAt": "2026-01-06T10:00:00Z",
  "description": "Payment service provider processing card transactions"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request data
- `500 Internal Server Error` - Server error

#### Using API Key

Include your API key in the `X-API-Key` header for all authenticated requests:

```http
X-API-Key: abc123xyz789def456ghi012jkl345mno678pqr901stu234vwx567yz
```

**Note:** API key authentication is currently optional for most endpoints. Future versions will require authentication for all endpoints.

---

## Transaction Validation & Screening

### Single Transaction Ingestion

Submit a transaction for real-time fraud detection and AML screening.

**Endpoint:** `POST /api/v1/transactions/ingest`

**Headers:**
```http
Content-Type: application/json
X-API-Key: your-api-key (optional)
```

**Request Body:**
```json
{
  "merchantId": "MERCH-001",
  "terminalId": "TERM-001",
  "amountCents": 10000,
  "currency": "USD",
  "pan": "4242424242424242",
  "isoMsg": "ISO8583 message (optional)",
  "emvTags": {
    "9F34": "020000",
    "82": "3F00"
  },
  "acquirerResponse": null
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `merchantId` | String | Yes | Unique merchant identifier |
| `terminalId` | String | No | Terminal/device identifier |
| `amountCents` | Long | Yes | Transaction amount in cents (must be positive) |
| `currency` | String | Yes | ISO 4217 currency code (e.g., USD, EUR) |
| `pan` | String | No | Primary Account Number (will be hashed) |
| `isoMsg` | String | No | Raw ISO 8583 message |
| `emvTags` | Object | No | EMV tag data as key-value pairs |
| `acquirerResponse` | String | No | Acquirer response code |

**Response:** `200 OK`
```json
{
  "txnId": 12345,
  "action": "APPROVE",
  "reasons": [
    "Transaction approved",
    "Risk score: 0.25 (below threshold)"
  ],
  "latencyMs": 45,
  "riskDetails": {
    "score": 0.25,
    "fraudScore": 0.15,
    "amlScore": 0.10,
    "velocityChecks": {
      "passed": true,
      "dailyCount": 5,
      "dailyAmount": 50000
    },
    "sanctionsCheck": {
      "passed": true,
      "matches": 0
    }
  }
}
```

**Action Values:**
- `APPROVE` - Transaction approved, proceed
- `HOLD` - Transaction held for review
- `REJECT` - Transaction rejected
- `ERROR` - Processing error occurred

**Response Codes:**
- `200 OK` - Transaction processed successfully
- `202 Accepted` - Transaction queued (high load)
- `400 Bad Request` - Invalid request data
- `429 Too Many Requests` - Rate limit exceeded
- `503 Service Unavailable` - Service temporarily unavailable
- `500 Internal Server Error` - Server error

### Batch Transaction Ingestion

Submit multiple transactions in a single request for improved throughput.

**Endpoint:** `POST /api/v1/transactions/ingest/batch`

**Request Body:**
```json
[
  {
    "merchantId": "MERCH-001",
    "terminalId": "TERM-001",
    "amountCents": 10000,
    "currency": "USD",
    "pan": "4242424242424242"
  },
  {
    "merchantId": "MERCH-001",
    "terminalId": "TERM-002",
    "amountCents": 25000,
    "currency": "USD",
    "pan": "5555555555554444"
  }
]
```

**Response:** `200 OK`
```json
[
  {
    "txnId": 12345,
    "action": "APPROVE",
    "reasons": ["Transaction approved"],
    "latencyMs": 42,
    "riskDetails": { ... }
  },
  {
    "txnId": 12346,
    "action": "HOLD",
    "reasons": ["High risk score: 0.75"],
    "latencyMs": 38,
    "riskDetails": { ... }
  }
]
```

**Note:** Batch requests are processed in parallel. Maximum batch size: 1000 transactions.

### Get Transaction Status

Retrieve transaction details by ID.

**Endpoint:** `GET /api/v1/transactions/{id}`

**Response:** `200 OK`
```json
{
  "txnId": 12345,
  "merchantId": "MERCH-001",
  "terminalId": "TERM-001",
  "amountCents": 10000,
  "currency": "USD",
  "panHash": "a1b2c3d4e5f6...",
  "txnTs": "2026-01-06T10:30:00Z",
  "pspId": 1,
  "createdAt": "2026-01-06T10:30:00Z"
}
```

### List Transactions

Retrieve a list of transactions with optional filtering.

**Endpoint:** `GET /api/v1/transactions`

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | Integer | No | 100 | Maximum number of transactions to return |
| `pspId` | Long | No | - | Filter by PSP ID |

**Example:**
```http
GET /api/v1/transactions?limit=50&pspId=1
```

**Response:** `200 OK`
```json
[
  {
    "txnId": 12345,
    "merchantId": "MERCH-001",
    "amountCents": 10000,
    "currency": "USD",
    "txnTs": "2026-01-06T10:30:00Z"
  },
  ...
]
```

### Health Check

Check transaction service health.

**Endpoint:** `GET /api/v1/transactions/health`

**Response:** `200 OK`
```
Transaction Service is running
```

---

## Merchant Onboarding

### Onboard New Merchant

Submit a merchant for onboarding, including automated screening and risk assessment.

**Endpoint:** `POST /api/v1/merchants/onboard`

**Headers:**
```http
Content-Type: application/json
Authorization: Bearer <token> (required)
```

**Request Body:**
```json
{
  "legalName": "Acme Corporation Ltd",
  "tradingName": "Acme Store",
  "country": "US",
  "registrationNumber": "12345678",
  "taxId": "12-3456789",
  "mcc": "5411",
  "businessType": "CORPORATION",
  "expectedMonthlyVolume": 100000000,
  "transactionChannel": "ONLINE",
  "website": "https://acme.com",
  "addressStreet": "123 Main St",
  "addressCity": "New York",
  "addressState": "NY",
  "addressPostalCode": "10001",
  "addressCountry": "US",
  "contactEmail": "contact@acme.com",
  "contactPhone": "+1234567890",
  "beneficialOwners": [
    {
      "fullName": "John Doe",
      "dateOfBirth": "1980-01-15",
      "nationality": "US",
      "ownershipPercentage": 100
    }
  ]
}
```

**Response:** `201 Created` (APPROVE), `202 Accepted` (REVIEW), `403 Forbidden` (REJECT)
```json
{
  "merchantId": 1001,
  "legalName": "Acme Corporation Ltd",
  "tradingName": "Acme Store",
  "status": "ACTIVE",
  "decision": "APPROVE",
  "riskScore": 25,
  "riskLevel": "LOW",
  "kycStatus": "COMPLETE",
  "screeningResult": {
    "hasMatches": false,
    "matchCount": 0,
    "screenedAt": "2026-01-06T10:00:00Z"
  },
  "createdAt": "2026-01-06T10:00:00Z"
}
```

**Decision Values:**
- `APPROVE` - Merchant approved, can process transactions
- `REVIEW` - Manual review required
- `REJECT` - Merchant rejected (high risk)

**Status Values:**
- `PENDING_SCREENING` - Initial status
- `ACTIVE` - Approved and active
- `UNDER_REVIEW` - Under compliance review
- `SUSPENDED` - Temporarily suspended
- `BLOCKED` - Permanently blocked

### Get Merchant Details

Retrieve merchant information by ID.

**Endpoint:** `GET /api/v1/merchants/{id}`

**Response:** `200 OK`
```json
{
  "merchantId": 1001,
  "legalName": "Acme Corporation Ltd",
  "tradingName": "Acme Store",
  "status": "ACTIVE",
  "riskLevel": "LOW",
  "kycStatus": "COMPLETE",
  "country": "US",
  "mcc": "5411",
  "dailyLimit": 1000000,
  "currentUsage": 50000
}
```

### List All Merchants

**Endpoint:** `GET /api/v1/merchants`

**Response:** `200 OK`
```json
[
  {
    "merchantId": 1001,
    "legalName": "Acme Corporation Ltd",
    "status": "ACTIVE",
    "riskLevel": "LOW"
  },
  ...
]
```

### Update Merchant

Update merchant information (triggers re-screening if significant changes).

**Endpoint:** `PUT /api/v1/merchants/{id}`

**Request Body:** (Same structure as onboarding, only include fields to update)

**Response:** `200 OK` - Updated merchant object

### Health Check

**Endpoint:** `GET /api/v1/merchants/health`

**Response:** `200 OK`
```
Merchant service is healthy
```

---

## Sanctions Screening

### Screen Name Against Sanctions

Screen a name (person or organization) against international sanctions lists.

**Endpoint:** `POST /api/v1/sanctions/screen`

**Request Body:**
```json
{
  "name": "John Doe",
  "entityType": "PERSON"
}
```

**Entity Types:**
- `PERSON` - Individual person
- `ORGANIZATION` - Company/organization
- `VESSEL` - Ship/vessel

**Response:** `200 OK`
```json
{
  "match": false,
  "status": "CLEAR",
  "matchCount": 0,
  "highestMatchScore": 0,
  "confidence": 0.0,
  "screenedName": "John Doe",
  "entityType": "PERSON",
  "screeningProvider": "AEROSPIKE",
  "screenedAt": "2026-01-06T10:00:00Z",
  "matches": [],
  "hits": []
}
```

**Match Response Example:**
```json
{
  "match": true,
  "status": "MATCH",
  "matchCount": 2,
  "highestMatchScore": 95,
  "confidence": 0.95,
  "screenedName": "John Doe",
  "entityType": "PERSON",
  "screeningProvider": "SUMSUB",
  "screenedAt": "2026-01-06T10:00:00Z",
  "matches": [
    {
      "name": "John Doe",
      "matchedName": "John Doe",
      "list": "UN Consolidated List",
      "sourceList": "UN Consolidated List",
      "listName": "UN Consolidated List",
      "similarityScore": 95,
      "reason": "SANCTIONS",
      "entityType": "PERSON"
    }
  ],
  "hits": [
    {
      "matchedName": "John Doe",
      "sourceList": "UN Consolidated List",
      "reason": "SANCTIONS"
    }
  ]
}
```

### Screen Person with Date of Birth

Screen a person with additional date of birth information for improved accuracy.

**Endpoint:** `POST /api/v1/sanctions/screen/person`

**Request Body:**
```json
{
  "fullName": "John Doe",
  "dateOfBirth": "1980-01-15"
}
```

**Response:** `200 OK` - Same format as `/screen` endpoint

### Screen Organization

Screen an organization (merchant/company) with legal and trading names.

**Endpoint:** `POST /api/v1/sanctions/screen/organization`

**Request Body:**
```json
{
  "legalName": "Acme Corporation Ltd",
  "tradingName": "Acme Store"
}
```

**Response:** `200 OK` - Same format as `/screen` endpoint

### Health Check

**Endpoint:** `GET /api/v1/sanctions/health`

**Response:** `200 OK`
```
Sanctions screening service is healthy
```

---

## Batch Operations

### Batch Screening

Screen multiple merchants in a single request.

**Endpoint:** `POST /api/v1/screening/batch`

**Request Body:**
```json
[
  {
    "legalName": "Acme Corporation",
    "tradingName": "Acme Store",
    "country": "US",
    "registrationNumber": "12345678"
  },
  {
    "legalName": "Beta Inc",
    "tradingName": "Beta Shop",
    "country": "CA",
    "registrationNumber": "87654321"
  }
]
```

**Response:** `200 OK`
```json
[
  {
    "hasMatches": false,
    "status": "CLEAR",
    "matchCount": 0,
    "screenedName": "Acme Corporation",
    "screenedAt": "2026-01-06T10:00:00Z"
  },
  {
    "hasMatches": false,
    "status": "CLEAR",
    "matchCount": 0,
    "screenedName": "Beta Inc",
    "screenedAt": "2026-01-06T10:00:00Z"
  }
]
```

**Note:** Batch screening uses async processing. Maximum batch size: 100 merchants.

---

## Monitoring & Reporting

### Transaction Monitoring Dashboard

Get real-time transaction monitoring statistics.

**Endpoint:** `GET /api/v1/monitoring/dashboard/stats`

**Response:** `200 OK`
```json
{
  "totalTransactions": 15000,
  "approvedTransactions": 14200,
  "heldTransactions": 600,
  "rejectedTransactions": 200,
  "averageLatencyMs": 45,
  "riskDistribution": {
    "LOW": 12000,
    "MEDIUM": 2500,
    "HIGH": 500
  }
}
```

### Risk Distribution

Get risk score distribution.

**Endpoint:** `GET /api/v1/monitoring/risk-distribution`

**Response:** `200 OK`
```json
{
  "low": 12000,
  "medium": 2500,
  "high": 500
}
```

### Recent Activity

Get recent transaction activity.

**Endpoint:** `GET /api/v1/monitoring/recent-activity?limit=50`

**Query Parameters:**
- `limit` - Number of records (default: 50)

**Response:** `200 OK`
```json
[
  {
    "txnId": 12345,
    "merchantId": "MERCH-001",
    "amountCents": 10000,
    "action": "APPROVE",
    "timestamp": "2026-01-06T10:30:00Z"
  },
  ...
]
```

### Transaction Reports

Get transaction reports with filtering.

**Endpoint:** `GET /api/v1/monitoring/transactions?startDate=2026-01-01&endDate=2026-01-31&pspId=1`

**Query Parameters:**
- `startDate` - Start date (ISO 8601)
- `endDate` - End date (ISO 8601)
- `pspId` - Filter by PSP ID
- `merchantId` - Filter by merchant ID
- `status` - Filter by status (APPROVE, HOLD, REJECT)

**Response:** `200 OK` - Array of transaction objects

### Summary Reports

Get summary reports.

**Endpoint:** `GET /api/v1/monitoring/reports/summary?period=MONTHLY&year=2026&month=1`

**Query Parameters:**
- `period` - Report period (DAILY, WEEKLY, MONTHLY, YEARLY)
- `year` - Year
- `month` - Month (1-12)
- `pspId` - Filter by PSP ID

**Response:** `200 OK`
```json
{
  "period": "MONTHLY",
  "year": 2026,
  "month": 1,
  "totalTransactions": 15000,
  "totalAmountCents": 1500000000,
  "approvedCount": 14200,
  "heldCount": 600,
  "rejectedCount": 200,
  "averageRiskScore": 0.25
}
```

---

## Pricing & Billing

### Get Pricing Tiers

Retrieve all available pricing tiers.

**Endpoint:** `GET /api/v1/pricing/tiers`

**Response:** `200 OK`
```json
[
  {
    "tierCode": "BASIC",
    "tierName": "Basic Plan",
    "monthlyFeeUsd": 99.00,
    "perCheckPriceUsd": 0.10,
    "monthlyMinimumUsd": 99.00,
    "maxChecksPerMonth": 10000,
    "includedChecks": 1000,
    "volumeDiscounts": {},
    "features": ["Basic screening", "Email support"]
  },
  {
    "tierCode": "PROFESSIONAL",
    "tierName": "Professional Plan",
    "monthlyFeeUsd": 299.00,
    "perCheckPriceUsd": 0.08,
    "monthlyMinimumUsd": 299.00,
    "maxChecksPerMonth": 100000,
    "includedChecks": 5000,
    "volumeDiscounts": {
      "50000": 0.06,
      "100000": 0.05
    },
    "features": ["Advanced screening", "Priority support", "API access"]
  }
]
```

### Get Specific Pricing Tier

**Endpoint:** `GET /api/v1/pricing/tiers/{tierCode}`

**Response:** `200 OK` - Single pricing tier object

### Estimate Cost

Estimate monthly cost for a pricing tier and expected usage.

**Endpoint:** `GET /api/v1/pricing/estimate?tier=PROFESSIONAL&checks=50000&currency=USD`

**Query Parameters:**
- `tier` - Pricing tier code (required)
- `checks` - Expected number of checks (required)
- `currency` - Currency code (default: USD)

**Response:** `200 OK`
```json
{
  "tierCode": "PROFESSIONAL",
  "estimatedChecks": 50000,
  "monthlyFee": 299.00,
  "perCheckCost": 0.08,
  "totalChecksCost": 4000.00,
  "volumeDiscount": 0.00,
  "totalCost": 4299.00,
  "currency": "USD"
}
```

### Compare Pricing Tiers

Compare costs across all tiers for given usage.

**Endpoint:** `GET /api/v1/pricing/compare?checks=50000&currency=USD`

**Response:** `200 OK` - Array of cost estimates for each tier

### Get Current Cost Metrics

**Endpoint:** `GET /api/v1/pricing/metrics/current`

**Response:** `200 OK`
```json
{
  "totalChecksThisMonth": 45000,
  "totalCostThisMonth": 3600.00,
  "averageCostPerCheck": 0.08,
  "currency": "USD"
}
```

### Get Billing History

**Endpoint:** `GET /api/v1/pricing/history/{pspId}`

**Response:** `200 OK`
```json
[
  {
    "pspId": 1,
    "period": "2026-01",
    "totalChecks": 45000,
    "totalCost": 3600.00,
    "currency": "USD",
    "calculatedAt": "2026-01-31T23:59:59Z"
  },
  ...
]
```

---

## Error Handling

### Standard Error Response Format

All error responses follow this format:

```json
{
  "error": "Error code or message",
  "message": "Human-readable error message",
  "timestamp": "2026-01-06T10:30:00Z",
  "path": "/api/v1/transactions/ingest"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `200` | OK | Request successful |
| `201` | Created | Resource created successfully |
| `202` | Accepted | Request accepted, processing asynchronously |
| `400` | Bad Request | Invalid request data or parameters |
| `401` | Unauthorized | Authentication required or failed |
| `403` | Forbidden | Insufficient permissions |
| `404` | Not Found | Resource not found |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Server error occurred |
| `503` | Service Unavailable | Service temporarily unavailable |

### Common Error Scenarios

#### Invalid Request Data
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Merchant ID is required",
  "timestamp": "2026-01-06T10:30:00Z",
  "path": "/api/v1/transactions/ingest"
}
```

#### Rate Limit Exceeded
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please retry after 60 seconds.",
  "timestamp": "2026-01-06T10:30:00Z",
  "path": "/api/v1/transactions/ingest"
}
```

#### Service Unavailable
```json
{
  "error": "SERVICE_UNAVAILABLE",
  "message": "Service temporarily unavailable, too many concurrent requests",
  "timestamp": "2026-01-06T10:30:00Z",
  "path": "/api/v1/transactions/ingest"
}
```

---

## Rate Limits & Best Practices

### Rate Limits

| Endpoint Category | Rate Limit | Window |
|------------------|------------|--------|
| Transaction Ingestion | 1000 requests/second | Per client |
| Batch Operations | 100 requests/minute | Per client |
| Sanctions Screening | 500 requests/second | Per client |
| Merchant Operations | 100 requests/minute | Per client |
| Reporting/Monitoring | 200 requests/minute | Per client |

**Rate Limit Headers:**

All responses include rate limit information:

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1641475200
```

### Best Practices

1. **Use Batch Endpoints:** For multiple operations, use batch endpoints instead of multiple single requests
2. **Implement Retry Logic:** Use exponential backoff for retries (1s, 2s, 4s, 8s)
3. **Handle Async Responses:** Some endpoints return `202 Accepted` - implement polling if needed
4. **Cache Pricing Data:** Pricing tiers change infrequently - cache for 1 hour
5. **Monitor Rate Limits:** Track `X-RateLimit-Remaining` header
6. **Error Handling:** Always check HTTP status codes and error responses
7. **Idempotency:** Use unique transaction IDs to prevent duplicate processing
8. **Connection Pooling:** Reuse HTTP connections for better performance
9. **Timeout Configuration:** Set appropriate timeouts (recommended: 30s connect, 60s read)
10. **Logging:** Log all API requests/responses for debugging and audit

### Performance Optimization

- **Batch Processing:** Use batch endpoints for multiple transactions
- **Async Processing:** Handle async responses appropriately
- **Connection Reuse:** Use HTTP connection pooling
- **Compression:** Enable gzip compression (supported automatically)

---

## Code Examples

### cURL Examples

#### Transaction Ingestion
```bash
curl -X POST https://api.your-domain.com/api/v1/transactions/ingest \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "merchantId": "MERCH-001",
    "terminalId": "TERM-001",
    "amountCents": 10000,
    "currency": "USD",
    "pan": "4242424242424242"
  }'
```

#### Sanctions Screening
```bash
curl -X POST https://api.your-domain.com/api/v1/sanctions/screen \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "name": "John Doe",
    "entityType": "PERSON"
  }'
```

#### Merchant Onboarding
```bash
curl -X POST https://api.your-domain.com/api/v1/merchants/onboard \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "legalName": "Acme Corporation Ltd",
    "country": "US",
    "registrationNumber": "12345678",
    "mcc": "5411",
    "businessType": "CORPORATION"
  }'
```

### Java Example

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AmlClient {
    private static final String BASE_URL = "https://api.your-domain.com/api/v1";
    private static final String API_KEY = "your-api-key";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AmlClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public FraudDetectionResponse ingestTransaction(TransactionRequest request) throws Exception {
        String json = objectMapper.writeValueAsString(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/transactions/ingest"))
            .header("Content-Type", "application/json")
            .header("X-API-Key", API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = httpClient.send(
            httpRequest, 
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), FraudDetectionResponse.class);
        } else {
            throw new RuntimeException("API Error: " + response.statusCode());
        }
    }
}
```

### Python Example

```python
import requests
import json

class AmlClient:
    def __init__(self, base_url, api_key):
        self.base_url = base_url
        self.api_key = api_key
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'X-API-Key': api_key
        })

    def ingest_transaction(self, transaction_data):
        url = f"{self.base_url}/transactions/ingest"
        response = self.session.post(url, json=transaction_data)
        response.raise_for_status()
        return response.json()

    def screen_sanctions(self, name, entity_type="PERSON"):
        url = f"{self.base_url}/sanctions/screen"
        data = {
            "name": name,
            "entityType": entity_type
        }
        response = self.session.post(url, json=data)
        response.raise_for_status()
        return response.json()

# Usage
client = AmlClient("https://api.your-domain.com/api/v1", "your-api-key")

# Ingest transaction
transaction = {
    "merchantId": "MERCH-001",
    "amountCents": 10000,
    "currency": "USD",
    "pan": "4242424242424242"
}
result = client.ingest_transaction(transaction)
print(f"Action: {result['action']}, Risk Score: {result['riskDetails']['score']}")

# Screen sanctions
screening_result = client.screen_sanctions("John Doe")
print(f"Match: {screening_result['match']}, Confidence: {screening_result['confidence']}")
```

### JavaScript/Node.js Example

```javascript
const axios = require('axios');

class AmlClient {
    constructor(baseUrl, apiKey) {
        this.baseUrl = baseUrl;
        this.client = axios.create({
            baseURL: baseUrl,
            headers: {
                'Content-Type': 'application/json',
                'X-API-Key': apiKey
            }
        });
    }

    async ingestTransaction(transactionData) {
        try {
            const response = await this.client.post('/transactions/ingest', transactionData);
            return response.data;
        } catch (error) {
            if (error.response) {
                throw new Error(`API Error: ${error.response.status} - ${error.response.data.message}`);
            }
            throw error;
        }
    }

    async screenSanctions(name, entityType = 'PERSON') {
        try {
            const response = await this.client.post('/sanctions/screen', {
                name,
                entityType
            });
            return response.data;
        } catch (error) {
            if (error.response) {
                throw new Error(`API Error: ${error.response.status} - ${error.response.data.message}`);
            }
            throw error;
        }
    }
}

// Usage
const client = new AmlClient('https://api.your-domain.com/api/v1', 'your-api-key');

// Ingest transaction
const transaction = {
    merchantId: 'MERCH-001',
    amountCents: 10000,
    currency: 'USD',
    pan: '4242424242424242'
};

client.ingestTransaction(transaction)
    .then(result => {
        console.log(`Action: ${result.action}, Risk Score: ${result.riskDetails.score}`);
    })
    .catch(error => {
        console.error('Error:', error.message);
    });

// Screen sanctions
client.screenSanctions('John Doe')
    .then(result => {
        console.log(`Match: ${result.match}, Confidence: ${result.confidence}`);
    })
    .catch(error => {
        console.error('Error:', error.message);
    });
```

---

## Support & Contact

### Technical Support

- **Email:** support@your-domain.com
- **Phone:** +1-800-XXX-XXXX
- **Hours:** Monday-Friday, 9 AM - 5 PM EST

### Documentation

- **API Reference:** https://docs.your-domain.com/api
- **Status Page:** https://status.your-domain.com
- **Changelog:** https://docs.your-domain.com/changelog

### Partner Portal

- **Portal URL:** https://portal.your-domain.com
- **Features:** API key management, usage analytics, billing history

### Emergency Support

For production incidents affecting transaction processing:
- **Email:** emergency@your-domain.com
- **Phone:** +1-800-XXX-XXXX (24/7)

---

## Appendix

### A. Supported Currencies

All ISO 4217 currency codes are supported. Common examples:
- USD (US Dollar)
- EUR (Euro)
- GBP (British Pound)
- JPY (Japanese Yen)
- CAD (Canadian Dollar)
- AUD (Australian Dollar)

### B. Merchant Category Codes (MCC)

Common MCC codes:
- `5411` - Grocery Stores, Supermarkets
- `5812` - Eating Places, Restaurants
- `5814` - Fast Food Restaurants
- `5999` - Miscellaneous and Specialty Retail Stores
- `6010` - Financial Institutions - Manual Cash Disbursements
- `6011` - Financial Institutions - Automated Cash Disbursements

### C. Entity Types

Supported entity types for sanctions screening:
- `PERSON` - Individual person
- `ORGANIZATION` - Company, corporation, or organization
- `VESSEL` - Ship, boat, or vessel

### D. Risk Levels

Risk level classifications:
- `LOW` - Low risk (0-30)
- `MEDIUM` - Medium risk (31-70)
- `HIGH` - High risk (71-100)

### E. Transaction Actions

Possible transaction actions:
- `APPROVE` - Transaction approved
- `HOLD` - Transaction held for review
- `REJECT` - Transaction rejected
- `ERROR` - Processing error

---

**Document Version:** 1.0  
**Last Updated:** January 6, 2026  
**Next Review:** April 6, 2026

