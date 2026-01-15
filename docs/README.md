# AML Fraud Detector

Anti-Money Laundering (AML) and Fraud Detection System built with Spring Boot and Java.

## Overview

This system provides comprehensive AML and fraud detection capabilities for payment gateway transactions. It follows a complete blueprint with:

- **Transaction Ingestion**: Receives and tracks all transactions from merchants
- **Feature Extraction**: Extracts behavioral, velocity, and EMV features
- **ML Scoring**: Integrates with XGBoost model for real-time fraud scoring
- **Decision Engine**: Applies configurable thresholds and rules from database
- **Alerting**: Creates alerts and cases for manual review
- **Monitoring**: Tracks model performance and metrics

## Architecture

```
Merchant → Transaction Ingestion → Aerospike (Primary Storage) + PostgreSQL (Backup)
                                  ↓
                            Feature Extraction → ML Scoring → Decision Engine → Action (BLOCK/HOLD/ALLOW) → Alerting
                                  ↓
                            UI Reads from Aerospike (with PostgreSQL fallback)
```

**Transaction Storage:** Transactions are stored in **Aerospike** for fast access (< 1ms latency) with PostgreSQL as backup for compliance. See [AEROSPIKE_TRANSACTION_STORAGE.md](docs/AEROSPIKE_TRANSACTION_STORAGE.md) for details.

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **REST Assured 5.3.2** (for all RESTful messaging)
- **PostgreSQL** (primary RDBMS for users, cases, audit)
- **Aerospike** (primary transaction storage for fast access)
- **Redis** (session cache, statistics)
- **Maven**

## Database Schema

The system uses a **multi-database architecture**:

### PostgreSQL (Primary RDBMS)
- `model_config`: Configurable thresholds and parameters (no hardcoding)
- `transactions`: Transaction backup/audit (primary storage in Aerospike)
- `transaction_features`: Features used for scoring and historical data
- `alerts`: Generated alerts and cases for review
- `model_metrics`: Model performance monitoring metrics
- `users`, `roles`, `compliance_cases`, `audit_logs`: User management and compliance data

### Aerospike (High-Performance Storage)
- **`transactions` namespace**: Primary transaction storage for fast access (< 1ms latency)
- `sanctions` namespace: Sanctions lists and watchlists
- `cache` namespace: Feature aggregates and hot data

**Note:** Transactions are stored in **Aerospike** as the primary storage for fast UI access, with PostgreSQL maintaining a backup copy for compliance and audit purposes. See [AEROSPIKE_TRANSACTION_STORAGE.md](docs/AEROSPIKE_TRANSACTION_STORAGE.md) for details.

## Configuration

**All configuration is done via `application.properties` or environment variables. No hardcoding.**

### Key Configuration Sections

- **AML Configuration**: `aml.*` properties
- **Fraud Detection**: `fraud.*` properties
- **Scoring Service**: `scoring.service.*` properties
- **Feature Extraction**: `feature.*` properties
- **Transaction Monitoring**: `transaction.monitoring.*` properties
- **Alert Configuration**: `alert.*` properties
- **Database**: `spring.datasource.*` properties

### Environment Variables

All properties can be overridden using environment variables:

```bash
export SERVER_PORT=8080
export DATABASE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export AML_ENABLED=true
export FRAUD_ENABLED=true
export SCORING_SERVICE_URL=http://localhost:8000
export FRAUD_THRESHOLD_BLOCK=0.95
export FRAUD_THRESHOLD_HOLD=0.7
```

### Database Configuration

Thresholds and rules are stored in the `model_config` table and can be updated at runtime:

```sql
-- Update fraud block threshold
UPDATE model_config SET value='0.92', updated_by='analyst1', updated_at=now() 
WHERE config_key='fraud.threshold.block';

-- View all configurations
SELECT config_key, value, description FROM model_config;
```

## Building and Running

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 13+
- Aerospike 6.0+ (for sanctions screening and caching)

### Build

```bash
mvn clean install
```

### Run (Development)

```bash
mvn spring-boot:run
```

Or with environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aml_fraud_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export SCORING_SERVICE_URL=http://localhost:8000
export AEROSPIKE_HOSTS=localhost:3000
mvn spring-boot:run
```

### Production Deployment

For production deployment, see **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** for detailed instructions including:
- Environment setup
- Database configuration
- Systemd service configuration
- Docker deployment
- Reverse proxy setup
- Monitoring and maintenance

## API Endpoints

### API Documentation

**Swagger UI:** Available at `http://localhost:2637/swagger-ui.html`  
**OpenAPI JSON:** Available at `http://localhost:2637/api-docs`

See **[SWAGGER_OPENAPI_SETUP.md](SWAGGER_OPENAPI_SETUP.md)** for details.

### Transaction Ingestion

- **POST** `/api/v1/transactions/ingest` - Receive and process transaction from merchant
- **GET** `/api/v1/transactions/health` - Health check

### Client Registration

- **POST** `/api/v1/clients/register` - Register a new client/user of the system
- **GET** `/api/v1/clients` - Get all registered clients
- **GET** `/api/v1/clients/active` - Get all active clients
- **GET** `/api/v1/clients/by-api-key/{apiKey}` - Get client by API key
- **PUT** `/api/v1/clients/{clientId}/status` - Update client status
- **GET** `/api/v1/clients/health` - Health check

### Merchant Management

- **GET** `/api/v1/merchants` - Get all merchants
- **GET** `/api/v1/merchants/{id}` - Get merchant by ID
- **POST** `/api/v1/merchants` - Create new merchant
- **PUT** `/api/v1/merchants/{id}` - Update merchant
- **DELETE** `/api/v1/merchants/{id}` - Delete merchant

### Case Management

- **GET** `/api/v1/compliance/cases` - Get all compliance cases
- **GET** `/api/v1/compliance/cases/{id}` - Get case by ID
- **GET** `/api/v1/cases/{caseId}/timeline` - Get case timeline
- **GET** `/api/v1/cases/{caseId}/network` - Get case network graph

### Risk Analytics

- **GET** `/api/v1/analytics/risk/heatmap/customer` - Customer risk heatmap
- **GET** `/api/v1/analytics/risk/heatmap/merchant` - Merchant risk heatmap
- **GET** `/api/v1/analytics/risk/heatmap/geographic` - Geographic risk heatmap
- **GET** `/api/v1/analytics/risk/trends` - Risk trend analysis

### Alert Management

- **GET** `/api/v1/alerts` - Get all alerts
- **GET** `/api/v1/alerts/{id}` - Get alert by ID
- **PUT** `/api/v1/alerts/{id}/resolve` - Resolve alert
- **GET** `/api/v1/alerts/disposition-stats` - Alert disposition statistics
- **POST** `/api/v1/alerts/tuning/suggest` - Suggest alert tuning
- **GET** `/api/v1/alerts/tuning/pending` - Get pending tuning recommendations

### Feedback and Labeling

- **POST** `/api/v1/feedback/label` - Label a transaction (fraud/not fraud)
- **POST** `/api/v1/feedback/label/batch` - Batch label multiple transactions
- **GET** `/api/v1/feedback/labeled` - Get all labeled transactions
- **GET** `/api/v1/feedback/unlabeled` - Get unlabeled transactions for review
- **GET** `/api/v1/feedback/statistics` - Get labeling statistics
- **GET** `/api/v1/feedback/health` - Health check

### Monitoring and Metrics

- **GET** `/api/v1/monitoring/metrics/latest` - Get latest model metrics
- **GET** `/api/v1/monitoring/metrics/date/{date}` - Get metrics for specific date
- **GET** `/api/v1/monitoring/metrics/range` - Get metrics for date range
- **POST** `/api/v1/monitoring/metrics/compute` - Trigger manual metrics computation
- **GET** `/api/v1/monitoring/health` - Health check

### Batch Processing

- **POST** `/api/v1/batch/score/yesterday` - Trigger batch scoring for yesterday's transactions
- **POST** `/api/v1/batch/backfill/features` - Backfill features for transactions
- **GET** `/api/v1/batch/health` - Health check

### Risk Assessment (Legacy)

- **POST** `/api/v1/risk-assessment/assess` - Assess risk for a transaction
- **GET** `/api/v1/risk-assessment/health` - Health check

### Example Client Registration Request

```json
POST /api/v1/clients/register
Content-Type: application/json

{
  "clientName": "Merchant ABC",
  "contactEmail": "contact@merchantabc.com",
  "contactPhone": "+1234567890",
  "description": "Retail merchant processing card transactions"
}
```

### Example Client Registration Response

```json
{
  "clientId": 1,
  "clientName": "Merchant ABC",
  "apiKey": "abc123xyz789...",
  "contactEmail": "contact@merchantabc.com",
  "contactPhone": "+1234567890",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:00:00",
  "description": "Retail merchant processing card transactions"
}
```

### Example Transaction Request

```json
POST /api/v1/transactions/ingest
Content-Type: application/json

{
  "isoMsg": "ISO8583 message here",
  "pan": "4242424242424242",
  "merchantId": "MERCH-001",
  "terminalId": "TERM-001",
  "amountCents": 10000,
  "currency": "USD",
  "txnTs": "2024-01-15T10:30:00",
  "emvTags": {
    "9F34": "020000",
    "82": "3F00",
    "4F": "A0000000041010"
  },
  "acquirerResponse": null
}
```

### Example Response

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

## ML Scoring Service Integration

The system integrates with an external ML scoring service (XGBoost model). The scoring service should:

1. Expose a REST endpoint: `POST /score`
2. Accept payload: `{"txn_id": 123, "features": {...}}`
3. Return: `{"txn_id": 123, "score": 0.85, "latency_ms": 10}`

### Python Scoring Service Example

The blueprint includes a Python FastAPI service for XGBoost scoring. To use it:

1. Train the model using the provided Python training script
2. Start the FastAPI scoring service on port 8000
3. Configure `scoring.service.url=http://localhost:8000` in application.properties

The Java service will automatically call the Python service for scoring.

## Features

### Complete Fraud Detection Pipeline

1. **Transaction Ingestion**: Receives and stores all merchant transactions
2. **Feature Extraction**: Extracts behavioral, velocity, EMV, and AML features
3. **ML Scoring**: Calls external XGBoost model for real-time scoring
4. **Decision Engine**: Applies configurable thresholds and rules
5. **Alerting**: Creates alerts for manual review
6. **Feedback Loop**: Investigators label transactions for model retraining
7. **Monitoring**: Tracks model performance metrics (AUC, precision@k, drift)
8. **Batch Processing**: Nightly batch scoring and feature backfilling
9. **User & Role Management**: RBAC with dynamic roles and PSP-level data isolation
10. **Web Dashboard**: Modern admin interface for monitoring and management
11. **Regulatory Reporting**: Automated IFTR (International Funds Transfer Report) generation
12. **Compliance Calendar**: Deadline management with automated notifications
13. **Dynamic Risk Configuration**: High-risk country management via database
14. **Document Retention**: Automated physical file cleanup policy

### Enhanced Features (Latest)

15. **Merchant Onboarding**: Complete merchant registration with KYC and sanctions screening
16. **Beneficial Ownership**: Management and screening of beneficial owners
17. **Case Management**: Full case lifecycle with timeline, network graph, and SLA tracking
18. **Dashboard Analytics**: Risk breakdown, transaction volume, fraud metrics, case aging, alert disposition
19. **Geographic Risk Heatmap**: Country-level risk visualization with interactive world map
20. **Alert Tuning**: Automated alert tuning recommendations and A/B testing
21. **Enhanced Error Handling**: Detailed error responses with error codes and trace IDs
22. **API Documentation**: Complete Swagger/OpenAPI documentation
23. **Connection Pooling**: Optimized HTTP connection management
24. **Circuit Breaker**: Resilience4j integration for fault tolerance
25. **Aerospike Integration**: High-performance sanctions screening and caching
26. **Database Query Optimization**: Comprehensive indexing and query performance monitoring
27. **Multi-Tier Caching**: Aerospike + Spring Cache for optimal performance
28. **Query Logging**: Configurable slow query detection and performance analysis
29. **Interactive Map Visualization**: Leaflet.js-based geographic risk heatmap
30. **Performance Monitoring**: Hibernate statistics and PostgreSQL query analysis
31. **Case Enrichment**: Automatic case enrichment with transaction links, merchant profiles, and risk details
32. **Graph Anomaly Detection**: Neo4j-based detection of circular trading patterns and mule proximity
33. **Case Archival & Retention**: Automated case archival with cold storage integration for compliance
34. **Rule & Model Versioning**: Track rule versions and model versions in alerts for auditability
35. **Database Indexing**: Optimized indexes on compliance cases for performance
36. **Grafana Monitoring**: Comprehensive Grafana dashboards with PSP-level segregation and role-based access
37. **Revenue Tracking**: Platform administrator dashboard for PSP revenue and transaction volume analysis
38. **PSP Isolation & Security**: Centralized `PspIsolationService` for multi-tenant data segregation
39. **Sanctions Testing**: Comprehensive test suite for sanctions download, loading, and screening
40. **Case Permission Service**: Enhanced PSP-aware case access control

### Feature Extraction

The system extracts comprehensive features including:

- **Transaction-level**: Amount, currency, merchant, terminal, time features
- **Behavioral**: Velocity features (txn count, amount sums), distinct terminals
- **EMV-specific**: Chip presence, contactless, CVM method, AIP flags
- **AML-specific**: Cumulative amounts, high-value transaction counts

### Decision Engine

The decision engine applies:

1. **Hard Rules** (checked first):
   - PAN blacklist
   - Terminal blacklist
   - High-risk MCC + high amount

2. **Model-based Rules** (from database):
   - Score >= block threshold → BLOCK
   - Score >= hold threshold → HOLD
   - Otherwise → ALLOW

3. **AML Rules**:
   - High-value transactions → ALERT
   - Cumulative amounts → Escalate
   - High-risk country check (Database-backed)
   - Sanctions screening (Aerospike-backed)

### Actions

- **BLOCK**: Decline transaction, create alert, optionally add to blocklist
- **HOLD**: Soft decline, create alert for manual review
- **ALERT**: Create case for compliance review
- **ALLOW**: Normal flow, transaction proceeds

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/posgateway/aml/
│   │       ├── config/          # Configuration classes
│   │       ├── controller/       # REST controllers
│   │       ├── entity/           # JPA entities
│   │       ├── repository/       # Data repositories
│   │       └── service/          # Business logic services
│   └── resources/
│       ├── application.properties # Application configuration
│       └── db/migration/         # Database migrations
└── test/
    └── java/                     # Test classes
```

## RESTful Messaging

All RESTful communication (both incoming and outgoing) uses **REST Assured**:

- **Incoming**: REST controllers receive requests (no authentication required for now)
- **Outgoing**: `RestClientService` uses REST Assured to call external services (e.g., ML scoring service)

### Client Registration

The system includes a client registration service to track users/clients of the system:

1. Clients register via `/api/v1/clients/register`
2. System generates a unique API key for each client
3. Client information is stored in the `clients` table
4. API keys can be used for future authentication (not implemented yet)

## Scheduled Tasks

The system includes scheduled tasks that run automatically:

- **Daily Metrics Computation**: Runs at midnight to compute model performance metrics
- **Batch Scoring**: Runs at 2 AM to score yesterday's transactions
- **Feature Backfilling**: Can be triggered manually or scheduled

## Development Guidelines

1. **No Hardcoding**: All values must be configurable via properties or database
2. **Java Only**: Use only Java and Spring Boot (convert any examples from other languages)
3. **REST Assured**: Use REST Assured for all RESTful messaging (incoming and outgoing)
4. **Security First**: All endpoints must be secured with appropriate `Permission` checks via `PermissionService`.
5. **Configuration First**: Always use `@ConfigurationProperties` or database config
6. **Database-Driven**: Thresholds and rules stored in `model_config` table
7. **Validation**: Use Jakarta Validation annotations for input validation
8. **Logging**: Use SLF4J for logging with appropriate log levels
9. **DTOs**: Use DTOs for API contracts (request/response)
10. **Exception Handling**: Global exception handler provides consistent error responses

## Database Initialization

The system includes a migration script that creates all tables and inserts default configuration values. Run:

```sql
-- Execute src/main/resources/db/migration/V1__Initial_Schema.sql
```

Or use Flyway/Liquibase for automatic migrations.

## Monitoring

The system tracks:

- Model performance metrics (AUC, precision@100)
- Average latency
- Feature drift scores
- Alert statistics
- Database query performance
- Cache hit rates
- Geographic risk distribution

Metrics are stored in the `model_metrics` table and can be exposed via Actuator endpoints.

### Performance Monitoring

**Database Query Monitoring:**
- Slow query logging (configurable threshold)
- Hibernate statistics
- PostgreSQL `pg_stat_statements` integration
- Query execution plan analysis

**Caching Performance:**
- Aerospike cache hit rates
- Spring Cache statistics
- Cache TTL monitoring
- Memory usage tracking

See **[DATABASE_QUERY_OPTIMIZATION.md](DATABASE_QUERY_OPTIMIZATION.md)** and **[CACHING_STRATEGY.md](CACHING_STRATEGY.md)** for detailed monitoring guides.

## Security Considerations

- **PAN Tokenization**: PANs are hashed (SHA-256) before storage
- **Role-Based Access Control (RBAC)**: Dynamic roles with granular permissions
- **PSP Data Isolation**: Multi-tenant data segregation enforced at all layers
  - PSP users can only access their own PSP's data
  - Platform Administrators can access all PSPs
  - Automatic PSP ID sanitization prevents parameter override
  - Defense-in-depth security with controller, service, and repository layers
- **Configuration Access**: Restricted via RBAC and permission checks
- **Scoring Service**: Should be on internal network with mTLS/JWT
- **Audit Logging**: All config changes and security violations are logged with user and timestamp
- **Security Audit**: Comprehensive PSP isolation audit completed (see `PSP_ISOLATION_SECURITY_AUDIT.md`)

## Additional Documentation

### Core Documentation
- **[01-Technical-Architecture.md](01-Technical-Architecture.md)** - System architecture and design
- **[02-Functional-Specification.md](02-Functional-Specification.md)** - Functional requirements
- **[03-Software-Requirements-Specification.md](03-Software-Requirements-Specification.md)** - Software requirements
- **[04-Software-Design-Document.md](04-Software-Design-Document.md)** - Detailed design specifications
- **[05-API-Reference.md](05-API-Reference.md)** - Complete API documentation
- **[06-Database-Design.md](06-Database-Design.md)** - Database schema and design
- **[07-User-Guide.md](07-User-Guide.md)** - End-user documentation
- **[08-Deployment-Guide.md](08-Deployment-Guide.md)** - Production deployment guide

### Security & Compliance
- **[PSP_ISOLATION_SECURITY_AUDIT.md](PSP_ISOLATION_SECURITY_AUDIT.md)** - PSP data isolation security audit
- **[PSP_METRICS_SEGREGATION.md](PSP_METRICS_SEGREGATION.md)** - PSP metrics segregation implementation
- **[GRAFANA_ROLE_BASED_ACCESS_SUMMARY.md](GRAFANA_ROLE_BASED_ACCESS_SUMMARY.md)** - Role-based access control summary

### Monitoring & Analytics
- **[GRAFANA_DASHBOARD_ACCESS_GUIDE.md](GRAFANA_DASHBOARD_ACCESS_GUIDE.md)** - Grafana dashboard access guide
- **[GRAFANA_QUICK_REFERENCE.md](GRAFANA_QUICK_REFERENCE.md)** - Quick reference for Grafana users
- **[PROMETHEUS_GRAFANA_SETUP.md](PROMETHEUS_GRAFANA_SETUP.md)** - Prometheus and Grafana setup
- **[REVENUE_DASHBOARD_GUIDE.md](REVENUE_DASHBOARD_GUIDE.md)** - Revenue tracking dashboard guide

### Scoring & Risk Assessment
- **[SCORING_PROCESS_DOCUMENTATION.md](SCORING_PROCESS_DOCUMENTATION.md)** - **Comprehensive documentation of all scoring systems** including detailed calculation formulas, component breakdowns, worked examples, and lookup tables for ML Score, KRS, TRS, CRA, Anomaly Detection, Fraud Detection, and AML Risk scores
- **[SCORE_TRACKING_IMPLEMENTATION.md](SCORE_TRACKING_IMPLEMENTATION.md)** - Implementation guide for score tracking across database, Kafka, Prometheus, and Grafana
- **[WEIGHTED_SCORING_SYSTEMS.md](WEIGHTED_SCORING_SYSTEMS.md)** - Detailed guide for KRS, TRS, and CRA weighted average scoring systems

### Data Storage & Performance
- **[AEROSPIKE_TRANSACTION_STORAGE.md](AEROSPIKE_TRANSACTION_STORAGE.md)** - **Transaction storage in Aerospike** for fast access (< 1ms latency). Includes architecture, configuration, migration guide, and performance benefits
- **[AEROSPIKE_CONNECTION_SETUP.md](AEROSPIKE_CONNECTION_SETUP.md)** - Aerospike connection setup and configuration
- **[CACHING_STRATEGY.md](CACHING_STRATEGY.md)** - Multi-tier caching strategy

### Implementation Guides
- **[SWAGGER_OPENAPI_SETUP.md](SWAGGER_OPENAPI_SETUP.md)** - API documentation setup
- **[ERROR_HANDLING_ENHANCEMENT.md](ERROR_HANDLING_ENHANCEMENT.md)** - Error handling implementation
- **[CACHING_STRATEGY.md](CACHING_STRATEGY.md)** - Multi-tier caching strategy
- **[DATABASE_QUERY_OPTIMIZATION.md](DATABASE_QUERY_OPTIMIZATION.md)** - Query optimization guide
- **[GEOGRAPHIC_MAP_IMPLEMENTATION.md](GEOGRAPHIC_MAP_IMPLEMENTATION.md)** - Geographic risk heatmap
- **[AEROSPIKE_CONNECTION_SETUP.md](AEROSPIKE_CONNECTION_SETUP.md)** - Aerospike setup and configuration

## License

Proprietary - Internal Use Only
