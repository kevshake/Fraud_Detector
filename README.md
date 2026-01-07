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
Merchant → Transaction Ingestion → Feature Extraction → ML Scoring → Decision Engine → Action (BLOCK/HOLD/ALLOW) → Alerting
```

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **REST Assured 5.3.2** (for all RESTful messaging)
- **PostgreSQL** (production)
- **Maven**

## Database Schema

The system uses PostgreSQL with the following tables:

- `model_config`: Configurable thresholds and parameters (no hardcoding)
- `transactions`: Raw transaction data from all merchants
- `transaction_features`: Features used for scoring and historical data
- `alerts`: Generated alerts and cases for review
- `model_metrics`: Model performance monitoring metrics

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

- PAN tokenization: PANs are hashed (SHA-256) before storage
- Configuration access: Should be restricted via RBAC
- Scoring service: Should be on internal network with mTLS/JWT
- Audit logging: All config changes are logged with user and timestamp

## Additional Documentation

- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Complete production deployment guide
- **[SWAGGER_OPENAPI_SETUP.md](SWAGGER_OPENAPI_SETUP.md)** - API documentation setup
- **[ERROR_HANDLING_ENHANCEMENT.md](ERROR_HANDLING_ENHANCEMENT.md)** - Error handling implementation details
- **[CACHING_STRATEGY.md](CACHING_STRATEGY.md)** - Multi-tier caching strategy and best practices
- **[DATABASE_QUERY_OPTIMIZATION.md](DATABASE_QUERY_OPTIMIZATION.md)** - Query optimization and performance monitoring
- **[GEOGRAPHIC_MAP_IMPLEMENTATION.md](GEOGRAPHIC_MAP_IMPLEMENTATION.md)** - Geographic risk heatmap map implementation
- **[COMPLETION_SUMMARY.md](COMPLETION_SUMMARY.md)** - Feature completion summary

## License

Proprietary - Internal Use Only
