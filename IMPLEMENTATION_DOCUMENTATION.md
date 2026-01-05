# AML Fraud Detector - Complete Implementation Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [Implemented Services](#implemented-services)
5. [REST API Endpoints](#rest-api-endpoints)
6. [Configuration](#configuration)
7. [Scheduled Tasks](#scheduled-tasks)
8. [Code Improvements](#code-improvements)
9. [Technology Stack](#technology-stack)
10. [Deployment Guide](#deployment-guide)

---

## Project Overview

**AML Fraud Detector** is a comprehensive Anti-Money Laundering (AML) and Fraud Detection System built with Spring Boot and Java. The system provides real-time transaction monitoring, ML-based fraud scoring, configurable decision rules, and a complete feedback loop for model retraining.

### Key Principles
- **No Hardcoding**: All values configurable via properties or database
- **Java Only**: Pure Java/Spring Boot implementation
- **REST Assured**: All RESTful messaging uses REST Assured
- **Database-Driven**: Thresholds and rules stored in database
- **Production-Ready**: Complete error handling, validation, and monitoring

---

## Architecture

### High-Level Flow

```
Merchant → Transaction Ingestion → Feature Extraction → ML Scoring → Decision Engine → Action (BLOCK/HOLD/ALLOW) → Alerting → Feedback Loop → Model Retraining
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    REST Controllers                         │
│  TransactionController │ ClientController │ FeedbackController │
│  MonitoringController │ BatchController │ RiskAssessmentController │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  TransactionIngestionService │ FraudDetectionOrchestrator │
│  FeatureExtractionService │ ScoringService │ DecisionEngine │
│  FeedbackLabelingService │ MonitoringMetricsService │ BatchScoringService │
│  ConfigService │ ClientRegistrationService │ RestClientService │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                        │
│  TransactionRepository │ TransactionFeaturesRepository │
│  AlertRepository │ ModelConfigRepository │ ModelMetricsRepository │
│  ClientRepository │                                        │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Database (PostgreSQL)                     │
│  transactions │ transaction_features │ alerts │ model_config │
│  model_metrics │ clients │                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Schema

### Tables Created

#### 1. `model_config`
Stores all configurable thresholds and parameters (no hardcoding).

**Columns:**
- `id` (SERIAL PRIMARY KEY)
- `config_key` (TEXT UNIQUE NOT NULL) - e.g., 'fraud.threshold.block'
- `value` (TEXT NOT NULL) - Configuration value
- `description` (TEXT) - Human-readable description
- `updated_by` (TEXT) - User who updated
- `updated_at` (TIMESTAMP) - Update timestamp

**Indexes:**
- `idx_config_key` on `config_key`

**Default Configurations:**
- `fraud.threshold.block` = '0.95'
- `fraud.threshold.hold` = '0.7'
- `fraud.rule.blacklist.enabled` = 'true'
- `aml.high_value_amount_cents` = '1000000'
- `fraud.action.block` = 'BLOCK'
- `fraud.notify.slack` = 'false'

#### 2. `transactions`
Stores raw transaction data from all merchants.

**Columns:**
- `txn_id` (BIGSERIAL PRIMARY KEY)
- `iso_msg` (TEXT) - Raw ISO8583 message
- `pan_hash` (TEXT) - SHA-256 hashed PAN (privacy)
- `merchant_id` (TEXT)
- `terminal_id` (TEXT)
- `amount_cents` (BIGINT)
- `currency` (CHAR(3))
- `txn_ts` (TIMESTAMP) - Transaction timestamp
- `emv_tags` (JSONB) - EMV tag data
- `acquirer_response` (TEXT)
- `created_at` (TIMESTAMP)

**Indexes:**
- `idx_txn_merchant` on `merchant_id`
- `idx_txn_timestamp` on `txn_ts`
- `idx_txn_pan_hash` on `pan_hash`

#### 3. `transaction_features`
Stores features used for model scoring and historical data.

**Columns:**
- `txn_id` (BIGINT PRIMARY KEY) - References transactions.txn_id
- `feature_json` (JSONB) - Feature map as JSON
- `score` (FLOAT) - ML model score
- `action_taken` (TEXT) - BLOCK, HOLD, ALLOW, ALERT
- `label` (SMALLINT) - 1=fraud, 0=good, NULL=unknown
- `scored_at` (TIMESTAMP)

**Indexes:**
- `idx_features_label` on `label`
- `idx_features_scored_at` on `scored_at`

#### 4. `alerts`
Stores generated alerts and cases for manual review.

**Columns:**
- `alert_id` (BIGSERIAL PRIMARY KEY)
- `txn_id` (BIGINT) - References transactions.txn_id
- `score` (FLOAT) - Fraud score that triggered alert
- `action` (TEXT) - BLOCK, HOLD, ALERT
- `reason` (TEXT) - Reason for alert
- `created_at` (TIMESTAMP)
- `status` (TEXT) - open, closed, false_positive
- `investigator` (TEXT) - Investigator assigned
- `notes` (TEXT) - Investigation notes

**Indexes:**
- `idx_alert_status` on `status`
- `idx_alert_created` on `created_at`
- `idx_alert_txn` on `txn_id`

#### 5. `model_metrics`
Stores model performance monitoring metrics.

**Columns:**
- `id` (SERIAL PRIMARY KEY)
- `date` (DATE) - Metrics date
- `auc` (FLOAT) - Area Under ROC Curve
- `precision_at_100` (FLOAT) - Precision at top 100
- `avg_latency_ms` (FLOAT) - Average scoring latency
- `drift_score` (FLOAT) - Model drift score
- `created_at` (TIMESTAMP)

**Indexes:**
- `idx_metrics_date` on `date`

#### 6. `clients`
Stores registered clients/users of the system.

**Columns:**
- `client_id` (SERIAL PRIMARY KEY)
- `client_name` (TEXT NOT NULL)
- `api_key` (TEXT UNIQUE NOT NULL) - Generated API key
- `contact_email` (TEXT NOT NULL)
- `contact_phone` (TEXT)
- `status` (TEXT NOT NULL) - ACTIVE, INACTIVE, SUSPENDED
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `last_accessed_at` (TIMESTAMP)
- `description` (TEXT)

**Indexes:**
- `idx_client_api_key` on `api_key`
- `idx_client_status` on `status`

---

## Implemented Services

### 1. TransactionIngestionService
**Purpose**: Receives and stores transactions from all merchants.

**Key Methods:**
- `ingestTransaction(TransactionRequest)` - Store transaction in database
- `hashPan(String)` - SHA-256 hash PAN for privacy

**Features:**
- PAN tokenization (SHA-256 hashing)
- EMV tag storage as JSONB
- Transaction timestamp handling
- ISO8583 message storage

### 2. FeatureExtractionService
**Purpose**: Extracts comprehensive features from transactions for ML scoring.

**Feature Categories:**

**Transaction-Level Features:**
- Amount (raw and log-transformed)
- Currency code
- Merchant ID, Terminal ID
- Card BIN (first 6 digits of PAN hash)
- Transaction hour of day, day of week

**Behavioral/Velocity Features:**
- Merchant transaction count (1 hour)
- Merchant amount sum (24 hours)
- PAN transaction count (1 hour)
- PAN amount sum (7 days)
- Distinct terminals (30 days for PAN)
- Average amount by PAN (30 days)
- Time since last transaction for PAN
- Z-score of amount vs PAN history

**EMV-Specific Features:**
- Chip present indicator
- Contactless indicator
- CVM method (from CVMR Tag 9F34)
- AIP flags (Tag 82)
- AID (Application ID - Tag 4F)
- Approval code present

**AML-Specific Features:**
- Cumulative debits (30 days)
- High-value transaction count (7 days)

**Performance:**
- Uses database queries for velocity features
- Caches aggregate features (via Spring Cache)
- Optimized queries with indexes

### 3. ScoringService
**Purpose**: Calls external ML scoring service (XGBoost model) using REST Assured.

**Key Methods:**
- `scoreTransaction(Long, Map<String, Object>)` - Score transaction via ML model

**Features:**
- REST Assured integration
- Retry logic (configurable max retries)
- Timeout handling
- Latency tracking
- Error handling with fallback

**Configuration:**
- `scoring.service.enabled` - Enable/disable scoring
- `scoring.service.url` - ML service URL
- `scoring.service.timeout.ms` - Request timeout
- `scoring.service.retry.max` - Max retry attempts

### 4. DecisionEngine
**Purpose**: Applies configurable thresholds and rules to determine transaction action.

**Decision Flow:**
1. **Hard Rules** (checked first):
   - PAN blacklist check
   - Terminal blacklist check
   - High-risk MCC + high amount

2. **Model-Based Rules** (from database):
   - Score >= block threshold → BLOCK
   - Score >= hold threshold → HOLD
   - Otherwise → ALLOW

3. **AML Rules**:
   - High-value transactions → ALERT
   - Cumulative amounts → Escalate

**Actions:**
- **BLOCK**: Decline transaction, create alert, optionally add to blocklist
- **HOLD**: Soft decline, create alert for manual review
- **ALERT**: Create case for compliance review
- **ALLOW**: Normal flow, transaction proceeds

**Features:**
- Database-driven thresholds (no hardcoding)
- Configurable action mappings
- Comprehensive logging
- Alert creation

### 5. FraudDetectionOrchestrator
**Purpose**: Orchestrates complete fraud detection pipeline.

**Pipeline Steps:**
1. Extract features from transaction
2. Score transaction using ML model
3. Make decision using decision engine
4. Return result with action and reasons

**Features:**
- End-to-end transaction processing
- Latency tracking
- Comprehensive logging
- Transaction management

### 6. FeedbackLabelingService
**Purpose**: Allows investigators to label transactions for model retraining.

**Key Methods:**
- `labelTransaction(Long, Short, String, String)` - Label single transaction
- `labelTransactionsBatch(Map<Long, Short>, String)` - Batch labeling
- `getLabeledTransactions()` - Get all labeled transactions
- `getUnlabeledTransactions(int)` - Get unlabeled transactions
- `getLabelingStatistics()` - Get labeling statistics

**Label Values:**
- `1` = Fraud
- `0` = Good
- `NULL` = Unknown

**Features:**
- Single and batch labeling
- Investigator tracking
- Labeling statistics
- Optimized statistics computation (single-pass algorithm)

**Code Optimizations:**
- Single-pass loop for statistics (instead of multiple streams)
- Switch-like pattern for label validation
- Cached label checks

### 7. MonitoringMetricsService
**Purpose**: Tracks model performance metrics and computes daily statistics.

**Key Methods:**
- `computeDailyMetrics()` - Scheduled daily metrics computation
- `computeAUC(List<TransactionFeatures>)` - Compute AUC
- `computePrecisionAtK(List<TransactionFeatures>, int)` - Compute precision@K
- `computeDriftScore(List<TransactionFeatures>)` - Compute drift score
- `getLatestMetrics()` - Get latest metrics
- `getMetricsForDate(LocalDate)` - Get metrics for date
- `getMetricsForDateRange(LocalDate, LocalDate)` - Get metrics for range

**Metrics Computed:**
- **AUC**: Area Under ROC Curve
- **Precision@100**: Precision at top 100 highest scores
- **Average Latency**: Average scoring latency
- **Drift Score**: Model drift detection

**Scheduled Task:**
- Runs daily at midnight (`@Scheduled(cron = "0 0 0 * * ?")`)

### 8. BatchScoringService
**Purpose**: Performs nightly batch scoring and feature backfilling.

**Key Methods:**
- `batchScoreYesterdayTransactions()` - Score yesterday's transactions
- `backfillFeatures(int)` - Backfill features for transactions without features

**Features:**
- Scheduled batch processing
- Skips already-scored transactions
- Feature backfilling
- Progress logging

**Scheduled Task:**
- Runs daily at 2 AM (`@Scheduled(cron = "0 0 2 * * ?")`)

### 9. ConfigService
**Purpose**: Manages configurable thresholds and parameters from database.

**Key Methods:**
- `getConfig(String, String)` - Get config value with default
- `getConfigAsDouble(String, Double)` - Get config as Double
- `getConfigAsInteger(String, Integer)` - Get config as Integer
- `getConfigAsBoolean(String, Boolean)` - Get config as Boolean
- `updateConfig(String, String, String, String)` - Update config
- `getFraudBlockThreshold()` - Get fraud block threshold
- `getFraudHoldThreshold()` - Get fraud hold threshold
- `getAmlHighValueThreshold()` - Get AML high value threshold
- `isBlacklistEnabled()` - Check if blacklist enabled

**Features:**
- Caching via Spring Cache (`@Cacheable`)
- Cache eviction on updates (`@CacheEvict`)
- Type-safe getters
- Default value fallbacks

### 10. ClientRegistrationService
**Purpose**: Manages client registration and API key generation.

**Key Methods:**
- `registerClient(ClientRegistrationRequest)` - Register new client
- `getClientByApiKey(String)` - Get client by API key
- `getAllActiveClients()` - Get all active clients
- `updateClientStatus(Long, String)` - Update client status
- `generateApiKey()` - Generate secure API key

**Features:**
- Secure API key generation (32 bytes, Base64 URL-safe)
- Unique API key validation
- Last accessed timestamp tracking
- Client status management

### 11. RestClientService
**Purpose**: Handles all outbound RESTful messaging using REST Assured.

**Key Methods:**
- `postRequest(String, Map<String, Object>)` - POST request
- `getRequest(String)` - GET request
- `postRequestWithRetry(String, Map<String, Object>, int)` - POST with retry

**Features:**
- REST Assured integration
- JSON request/response handling
- Retry logic with exponential backoff
- Error handling

### 12. RegulatoryReportingService
**Purpose**: Generates regulatory reports like IFTR.

**Key Methods:**
- `generateIftr(Long)` - Generates International Funds Transfer Report based on transaction details.

### 13. CaseEscalationService
**Purpose**: Handles automatic case risk scoring and escalation.

**Key Methods:**
- `getCaseRiskScore(Long)` - Calculates aggregate risk score for a case.
- `getCaseTotalAmount(Long)` - Sums up all transaction amounts associated with a case.

### 14. DocumentRetentionService
**Purpose**: Manages document lifecycle and physical file deletion.

**Key Methods:**
- `cleanupExpiredDocuments()` - Deletes expired document records and their physical files from the filesystem.

### 15. RealTimeTransactionScreeningService
**Purpose**: Performs real-time screening of transactions.

**Key Methods:**
- `extractCounterpartyName(String)` - Extracts counterparty name from ISO8583 messages for screening.

### 16. CustomerRiskProfilingService & MerchantOnboardingService
**Enhancement**: Externalized high-risk country list to database-backed `HighRiskCountryRepository` for dynamic updates without code changes.

### 17. AmlService (Legacy)
**Purpose**: Legacy AML risk assessment service.

**Key Methods:**
- `assessAmlRisk(Transaction)` - Assess AML risk

### 13. FraudDetectionService (Legacy)
**Purpose**: Legacy fraud detection service.

**Key Methods:**
- `assessFraudRisk(Transaction)` - Assess fraud risk

### 14. RiskAssessmentService (Legacy)
**Purpose**: Legacy risk assessment orchestrator.

**Key Methods:**
- `assessRisk(Transaction)` - Combined AML and fraud assessment

---

## REST API Endpoints

### Transaction Endpoints

#### POST `/api/v1/transactions/ingest`
Receive and process transaction from merchant.

**Request Body:**
```json
{
  "merchantId": "MERCH-001",
  "terminalId": "TERM-001",
  "amountCents": 10000,
  "currency": "USD",
  "pan": "4242424242424242",
  "isoMsg": "ISO8583 message",
  "emvTags": {
    "9F34": "020000",
    "82": "3F00"
  },
  "acquirerResponse": null
}
```

**Response:**
```json
{
  "txnId": 12345,
  "score": 0.85,
  "action": "HOLD",
  "reasons": ["Score 0.850 >= hold threshold 0.700"],
  "latencyMs": 45
}
```

#### GET `/api/v1/transactions/health`
Health check endpoint.

### Client Registration Endpoints

#### POST `/api/v1/clients/register`
Register a new client/user of the system.

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
  "contactEmail": "contact@merchantabc.com",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:00:00"
}
```

#### GET `/api/v1/clients`
Get all registered clients.

#### GET `/api/v1/clients/active`
Get all active clients.

#### GET `/api/v1/clients/by-api-key/{apiKey}`
Get client by API key.

#### PUT `/api/v1/clients/{clientId}/status?status=ACTIVE`
Update client status.

#### GET `/api/v1/clients/health`
Health check endpoint.

### Feedback Endpoints

#### POST `/api/v1/feedback/label`
Label a transaction (fraud/not fraud).

**Request Body:**
```json
{
  "txnId": 12345,
  "label": 1,
  "investigator": "investigator1",
  "notes": "Confirmed fraud case"
}
```

**Response:**
```json
{
  "txnId": 12345,
  "score": 0.85,
  "actionTaken": "HOLD",
  "label": 1,
  "scoredAt": "2024-01-15T10:30:00"
}
```

#### POST `/api/v1/feedback/label/batch?investigator=investigator1`
Batch label multiple transactions.

**Request Body:**
```json
{
  "12345": 1,
  "12346": 0,
  "12347": 1
}
```

**Response:**
```json
{
  "labeled": 3
}
```

#### GET `/api/v1/feedback/labeled`
Get all labeled transactions.

#### GET `/api/v1/feedback/unlabeled?limit=100`
Get unlabeled transactions for review.

#### GET `/api/v1/feedback/statistics`
Get labeling statistics.

**Response:**
```json
{
  "totalTransactions": 10000,
  "labeledTransactions": 5000,
  "fraudTransactions": 500,
  "goodTransactions": 4500,
  "unlabeledTransactions": 5000,
  "labelingRate": 0.5
}
```

#### GET `/api/v1/feedback/health`
Health check endpoint.

### Monitoring Endpoints

#### GET `/api/v1/monitoring/metrics/latest`
Get latest model metrics.

**Response:**
```json
{
  "id": 1,
  "date": "2024-01-14",
  "auc": 0.95,
  "precisionAt100": 0.85,
  "avgLatencyMs": 45.5,
  "driftScore": 0.02,
  "createdAt": "2024-01-15T00:00:00"
}
```

#### GET `/api/v1/monitoring/metrics/date/2024-01-14`
Get metrics for specific date.

#### GET `/api/v1/monitoring/metrics/range?startDate=2024-01-01&endDate=2024-01-31`
Get metrics for date range.

#### POST `/api/v1/monitoring/metrics/compute`
Trigger manual metrics computation.

#### GET `/api/v1/monitoring/health`
Health check endpoint.

### Batch Processing Endpoints

#### POST `/api/v1/batch/score/yesterday`
Trigger batch scoring for yesterday's transactions.

**Response:**
```
"Batch scoring completed"
```

#### POST `/api/v1/batch/backfill/features?limit=1000`
Backfill features for transactions without features.

**Response:**
```json
{
  "processed": 1000
}
```

#### GET `/api/v1/batch/health`
Health check endpoint.

### Risk Assessment Endpoints (Legacy)

#### POST `/api/v1/risk-assessment/assess`
Assess risk for a transaction (legacy endpoint).

#### GET `/api/v1/risk-assessment/health`
Health check endpoint.

---

## Configuration

### Application Properties

All configuration in `src/main/resources/application.properties`:

#### Application Configuration
```properties
spring.application.name=aml-fraud-detector
server.port=${SERVER_PORT:8080}
server.servlet.context-path=${CONTEXT_PATH:/api/v1}
```

#### Database Configuration
```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/aml_fraud_db}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
```

#### AML Configuration
```properties
aml.enabled=${AML_ENABLED:true}
aml.risk.threshold.low=${AML_RISK_THRESHOLD_LOW:30}
aml.risk.threshold.medium=${AML_RISK_THRESHOLD_MEDIUM:60}
aml.risk.threshold.high=${AML_RISK_THRESHOLD_HIGH:80}
```

#### Fraud Detection Configuration
```properties
fraud.enabled=${FRAUD_ENABLED:true}
fraud.scoring.enabled=${FRAUD_SCORING_ENABLED:true}
fraud.scoring.threshold=${FRAUD_SCORING_THRESHOLD:70}
fraud.velocity.check.enabled=${FRAUD_VELOCITY_CHECK:true}
fraud.velocity.window.minutes=${FRAUD_VELOCITY_WINDOW:60}
fraud.velocity.max.transactions=${FRAUD_VELOCITY_MAX_TX:10}
```

#### Scoring Service Configuration
```properties
scoring.service.enabled=${SCORING_SERVICE_ENABLED:true}
scoring.service.url=${SCORING_SERVICE_URL:http://localhost:8000}
scoring.service.timeout.ms=${SCORING_SERVICE_TIMEOUT:200}
scoring.service.retry.max=${SCORING_SERVICE_RETRY_MAX:3}
```

#### Feature Extraction Configuration
```properties
feature.extraction.enabled=${FEATURE_EXTRACTION_ENABLED:true}
feature.aggregate.cache.enabled=${FEATURE_AGGREGATE_CACHE:true}
feature.velocity.window.hours=${FEATURE_VELOCITY_WINDOW_HOURS:1}
feature.velocity.window.days=${FEATURE_VELOCITY_WINDOW_DAYS:7}
```

#### Cache Configuration
```properties
spring.cache.type=${CACHE_TYPE:simple}
spring.cache.cache-names=modelConfig,aggregateFeatures
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=300s
```

### Database Configuration

All thresholds stored in `model_config` table:

```sql
-- Update fraud block threshold
UPDATE model_config 
SET value='0.92', updated_by='analyst1', updated_at=now() 
WHERE config_key='fraud.threshold.block';

-- View all configurations
SELECT config_key, value, description FROM model_config;
```

---

## Scheduled Tasks

### 1. Daily Metrics Computation
**Service**: `MonitoringMetricsService.computeDailyMetrics()`
**Schedule**: Daily at midnight (`@Scheduled(cron = "0 0 0 * * ?")`)
**Purpose**: Compute model performance metrics for previous day

**Actions:**
- Retrieves transactions scored yesterday
- Computes AUC, precision@100, average latency, drift score
- Stores metrics in `model_metrics` table

### 2. Batch Scoring
**Service**: `BatchScoringService.batchScoreYesterdayTransactions()`
**Schedule**: Daily at 2 AM (`@Scheduled(cron = "0 0 2 * * ?")`)
**Purpose**: Score yesterday's transactions in batch

**Actions:**
- Retrieves all transactions from yesterday
- Skips already-scored transactions
- Extracts features and scores each transaction
- Saves features and scores to database

### 3. Feature Backfilling
**Service**: `BatchScoringService.backfillFeatures(int)`
**Schedule**: Manual trigger or can be scheduled
**Purpose**: Backfill features for transactions without features

**Actions:**
- Finds transactions without features
- Extracts and saves features
- Processes up to specified limit

---

## Code Improvements

### Performance Optimizations

1. **FeedbackLabelingService.getLabelingStatistics()**
   - Changed from multiple stream operations to single-pass loop
   - Reduces iterations from O(3n) to O(n)
   - Caches label checks

2. **FeedbackLabelingService.labelTransaction()**
   - Optimized label validation using switch-like pattern
   - Better performance for label description

3. **ScoringService.extractDouble() and extractLong()**
   - Early return for null values
   - Optimized instanceof checks (Number interface)
   - Fallback to string parsing only when needed

### Code Quality Improvements

1. **DTO Classes**
   - Created `TransactionRequestDTO`, `FraudDetectionResponseDTO`, `LabelTransactionRequestDTO`
   - Better API contracts
   - Validation annotations

2. **Global Exception Handler**
   - Centralized exception handling
   - Consistent error responses
   - Proper HTTP status codes

3. **Validation**
   - Jakarta Validation annotations on DTOs
   - Input validation for all endpoints
   - Clear error messages

4. **Logging**
   - Comprehensive logging throughout
   - Appropriate log levels (DEBUG, INFO, WARN, ERROR)
   - Structured log messages

5. **Documentation**
   - JavaDoc comments on all public methods
   - Clear method descriptions
   - Parameter and return value documentation

---

## Technology Stack

### Core Dependencies

- **Spring Boot**: 3.2.0
- **Java**: 21
- **Spring Data JPA**: Data access layer
- **Spring Web**: REST API support
- **Spring WebFlux**: Reactive support (for REST Assured compatibility)
- **REST Assured**: 5.3.2 - All RESTful messaging
- **PostgreSQL**: Production database
- **Jackson**: JSON processing
- **Jakarta Validation**: Input validation
- **SLF4J**: Logging

### Build Tool

- **Maven**: Dependency management and build

---

## Deployment Guide

### Prerequisites

1. Java 17 or higher
2. Maven 3.6+
3. PostgreSQL (primary database)
4. Python ML Scoring Service (optional, for XGBoost model)

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

### Environment Variables

```bash
export SERVER_PORT=8080
export DATABASE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export SCORING_SERVICE_URL=http://localhost:8000
export AML_ENABLED=true
export FRAUD_ENABLED=true
```

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE aml_fraud_db;
```

2. Run migration script:
```bash
psql -U postgres -d aml_fraud_db -f src/main/resources/db/migration/V1__Initial_Schema.sql
```

Or let JPA auto-create tables (set `spring.jpa.hibernate.ddl-auto=update`)

### Health Checks

All services provide health check endpoints:
- `/api/v1/transactions/health`
- `/api/v1/clients/health`
- `/api/v1/feedback/health`
- `/api/v1/monitoring/health`
- `/api/v1/batch/health`
- `/api/v1/risk-assessment/health`

### Monitoring

- Actuator endpoints: `/actuator/health`, `/actuator/metrics`
- Model metrics: `/api/v1/monitoring/metrics/latest`
- Labeling statistics: `/api/v1/feedback/statistics`

---

## File Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/posgateway/aml/
│   │       ├── AmlFraudDetectorApplication.java
│   │       ├── config/
│   │       │   ├── AmlProperties.java
│   │       │   ├── FraudProperties.java
│   │       │   ├── TransactionMonitoringProperties.java
│   │       │   ├── AlertProperties.java
│   │       │   └── CacheConfig.java
│   │       ├── controller/
│   │       │   ├── TransactionController.java
│   │       │   ├── ClientController.java
│   │       │   ├── FeedbackController.java
│   │       │   ├── MonitoringController.java
│   │       │   ├── BatchController.java
│   │       │   └── RiskAssessmentController.java
│   │       ├── dto/
│   │       │   ├── TransactionRequestDTO.java
│   │       │   ├── FraudDetectionResponseDTO.java
│   │       │   └── LabelTransactionRequestDTO.java
│   │       ├── entity/
│   │       │   ├── TransactionEntity.java
│   │       │   ├── TransactionFeatures.java
│   │       │   ├── Alert.java
│   │       │   ├── ModelConfig.java
│   │       │   ├── ModelMetrics.java
│   │       │   └── Client.java
│   │       ├── exception/
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── model/
│   │       │   ├── Transaction.java
│   │       │   ├── TransactionStatus.java
│   │       │   ├── RiskLevel.java
│   │       │   └── RiskAssessment.java
│   │       ├── repository/
│   │       │   ├── TransactionRepository.java
│   │       │   ├── TransactionFeaturesRepository.java
│   │       │   ├── AlertRepository.java
│   │       │   ├── ModelConfigRepository.java
│   │       │   ├── ModelMetricsRepository.java
│   │       │   └── ClientRepository.java
│   │       └── service/
│   │           ├── TransactionIngestionService.java
│   │           ├── FeatureExtractionService.java
│   │           ├── ScoringService.java
│   │           ├── DecisionEngine.java
│   │           ├── FraudDetectionOrchestrator.java
│   │           ├── FeedbackLabelingService.java
│   │           ├── MonitoringMetricsService.java
│   │           ├── BatchScoringService.java
│   │           ├── ConfigService.java
│   │           ├── ClientRegistrationService.java
│   │           ├── RestClientService.java
│   │           ├── AmlService.java
│   │           ├── FraudDetectionService.java
│   │           └── RiskAssessmentService.java
│   └── resources/
│       ├── application.properties
│       └── db/migration/
│           └── V1__Initial_Schema.sql
└── test/
    └── java/
```

---

## Summary

### Implemented Features

✅ **Transaction Ingestion**: Receive and store all merchant transactions
✅ **Feature Extraction**: Comprehensive feature extraction (transaction, behavioral, EMV, AML)
✅ **ML Scoring Integration**: REST Assured integration with external XGBoost model
✅ **Decision Engine**: Database-configurable thresholds and rules
✅ **Alerting**: Alert creation and case management
✅ **Feedback Loop**: Transaction labeling for model retraining
✅ **Monitoring**: Model performance metrics tracking
✅ **Batch Processing**: Nightly batch scoring and feature backfilling
✅ **Client Registration**: Client/user management with API keys
✅ **REST Assured**: All RESTful messaging via REST Assured
✅ **Error Handling**: Global exception handler
✅ **Validation**: Comprehensive input validation
✅ **Scheduled Tasks**: Daily metrics computation and batch scoring
✅ **DTOs**: Clean API contracts
✅ **Performance Optimizations**: Single-pass algorithms, optimized checks

### Code Quality

- ✅ Zero hardcoding (all configurable)
- ✅ Comprehensive error handling
- ✅ Input validation
- ✅ Logging throughout
- ✅ JavaDoc documentation
- ✅ Clean architecture
- ✅ Separation of concerns
- ✅ Database-driven configuration

### Production Ready

- ✅ Database migrations
- ✅ Health check endpoints
- ✅ Monitoring and metrics
- ✅ Scheduled tasks
- ✅ Error handling
- ✅ Logging
- ✅ Configuration management

---

## Version History

- **v1.0.0** (Current)
  - Initial implementation
  - Complete fraud detection pipeline
  - Feedback loop
  - Monitoring and metrics
  - Batch processing
  - Client registration
  - REST Assured integration

---

**Document Generated**: 2024-01-15
**Last Updated**: 2025-12-29
**Status**: Production Ready ✅

