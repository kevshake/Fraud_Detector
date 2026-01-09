# Technical Architecture Document
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026  
**Status:** Production-Ready

---

## 1. Executive Summary

The AML Fraud Detector is an enterprise-grade Anti-Money Laundering (AML) and Fraud Detection system designed for payment gateway operations. It provides real-time transaction monitoring, risk assessment, sanctions screening, and comprehensive compliance management.

### Key Capabilities
- Real-time transaction fraud scoring (< 200ms response)
- AML/CFT compliance monitoring
- Sanctions screening against global watchlists
- Case management and workflow automation
- Regulatory reporting (SAR, IFTR)
- High-throughput processing (30,000+ concurrent requests)

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                                │
├──────────────┬───────────────┬───────────────┬───────────────┬──────────────┤
│   Merchants  │   POS/ATM     │  ML Scoring   │   Sumsub      │   Partners   │
│   Systems    │   Terminals   │   Service     │   (KYC)       │   (PSPs)     │
└──────┬───────┴───────┬───────┴───────┬───────┴───────┬───────┴──────┬───────┘
       │               │               │               │              │
       ▼               ▼               ▼               ▼              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           API GATEWAY LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  REST Controllers (Spring MVC) - Port 2637                          │   │
│  │  • Transaction Ingestion    • Case Management    • User Management  │   │
│  │  • Alert Management         • Reporting          • Analytics        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BUSINESS LOGIC LAYER                                │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────────┐    │
│  │  Transaction   │  │  AML Service   │  │  Fraud Detection Service  │    │
│  │  Orchestrator  │──│  • Velocity    │──│  • Feature Extraction     │    │
│  │                │  │  • Sanctions   │  │  • ML Scoring             │    │
│  │                │  │  • Patterns    │  │  • Decision Engine        │    │
│  └────────────────┘  └────────────────┘  └────────────────────────────┘    │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────────┐    │
│  │  Case Mgmt     │  │  Compliance    │  │  Reporting Service        │    │
│  │  • Workflow    │  │  • Calendar    │  │  • SAR Generation         │    │
│  │  • Assignment  │  │  • Deadlines   │  │  • Analytics              │    │
│  │  • Enrichment  │  │                │  │                            │    │
│  │  • Archival    │  │                │  │                            │    │
│  └────────────────┘  └────────────────┘  └────────────────────────────┘    │
│  ┌────────────────┐  ┌──────────────────────────────────────────────────┐    │
│  │  Graph Analytics│  │  Neo4j Graph Data Science (GDS)                  │    │
│  │  • Cycle Detect │  │  • Circular Trading Detection                    │    │
│  │  • Mule Detect  │  │  • Mule Proximity Analysis                       │    │
│  │  • Risk Update  │  │  • Network Anomaly Detection                     │    │
│  └────────────────┘  └──────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATA ACCESS LAYER                                   │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────────┐    │
│  │  JPA/Hibernate │  │  Aerospike     │  │  Redis                    │    │
│  │  Repositories  │  │  Client        │  │  Template                 │    │
│  └────────┬───────┘  └────────┬───────┘  └────────────┬───────────────┘    │
└───────────┼───────────────────┼────────────────────────┼────────────────────┘
            │                   │                        │
            ▼                   ▼                        ▼
┌────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
│    PostgreSQL      │ │    Aerospike       │ │       Redis        │
│  ─────────────     │ │  ─────────────     │ │  ─────────────     │
│  • Transactions    │ │  • Sanctions List  │ │  • Statistics      │
│  • Users/Roles     │ │  • Feature Cache   │ │  • Session Cache   │
│  • Cases/Alerts    │ │  • Hot Data        │ │  • Rate Limits     │
│  • Audit Logs      │ │                    │ │                    │
└────────────────────┘ └────────────────────┘ └────────────────────┘
```

### 2.2 Component Architecture

```
com.posgateway.aml/
├── config/              # Configuration classes (27 files)
│   ├── SecurityConfig   # Spring Security configuration with PSP-aware access control
│   ├── CacheConfig      # Caching strategy configuration
│   ├── AsyncConfig      # Async processing configuration
│   └── ...
├── service/
│   ├── security/        # Security services
│   │   └── PspIsolationService  # PSP data isolation enforcement
│   ├── case_management/ # Case management services
│   │   ├── CaseEnrichmentService  # Case enrichment with context
│   │   ├── CasePermissionService  # PSP-aware case access control
│   │   └── CaseAuditService       # Immutable audit logging
│   └── ...
├── controller/          # REST API endpoints (51 controllers)
│   ├── /api/v1/transactions
│   ├── /api/v1/merchants
│   ├── /api/v1/compliance/cases
│   └── ...
├── service/             # Business logic (135 services)
│   ├── core/            # Core fraud/AML services
│   ├── case_management/ # Case workflow services
│   ├── analytics/       # Analytics and reporting
│   └── ...
├── entity/              # JPA entities (62 entities)
│   ├── merchant/        # Merchant domain
│   ├── compliance/      # Compliance domain
│   ├── sanctions/       # Sanctions domain
│   └── ...
├── repository/          # Data access (61 repositories)
├── dto/                 # Data transfer objects (25 DTOs)
├── mapper/              # MapStruct mappers (4 mappers)
└── util/                # Utilities (2 files)
```

---

## 3. Technology Stack

### 3.1 Backend Technologies

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Language | Java | 17+ | Core development language |
| Framework | Spring Boot | 3.2.0 | Application framework |
| Security | Spring Security | 6.x | Authentication/Authorization |
| ORM | Hibernate/JPA | 6.x | Object-relational mapping |
| Build | Maven | 3.6+ | Dependency management |
| JSON | Jackson | 2.x | Serialization/Deserialization |
| Mapping | MapStruct | 1.5.5 | DTO mapping |
| Resilience | Resilience4j | 2.1.0 | Circuit breakers, retries |

### 3.2 Data Storage

| Database | Purpose | Configuration |
|----------|---------|---------------|
| PostgreSQL 13+ | Primary transactional data | JDBC + HikariCP |
| Aerospike 6.0+ | Sanctions screening, caching | Aerospike Java Client 9.2.0 |
| Redis | Session cache, statistics | Spring Data Redis |

### 3.3 Frontend Technologies

| Component | Technology | Purpose |
|-----------|------------|---------|
| Structure | HTML5 | Page structure |
| Logic | ES6+ JavaScript | Application logic |
| Styling | CSS3 + Variables | Theming (Glassmorphism) |
| Charts | Chart.js | Data visualization |
| Maps | Leaflet.js | Geographic risk heatmap |
| Icons | FontAwesome 6 | UI iconography |

---

## 4. Processing Pipelines

### 4.1 Transaction Processing Pipeline

```
Transaction Received
        │
        ▼
┌───────────────────┐
│ Transaction       │
│ Ingestion Service │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐     ┌───────────────────┐
│ Feature           │────▶│ Statistics        │
│ Extraction        │     │ Recording         │
└────────┬──────────┘     └───────────────────┘
         │
         ▼
┌───────────────────┐     ┌───────────────────┐
│ ML Scoring        │────▶│ External XGBoost  │
│ Service           │     │ Model Service     │
└────────┬──────────┘     └───────────────────┘
         │
         ▼
┌───────────────────┐
│ Decision Engine   │
│ • Hard Rules      │
│ • Model Thresholds│
│ • AML Rules       │
└────────┬──────────┘
         │
         ▼
┌───────────────────────────────────────────┐
│                 DECISION                   │
├───────────┬───────────┬───────────────────┤
│   BLOCK   │   HOLD    │  ALERT  │  ALLOW  │
└───────────┴───────────┴─────────┴─────────┘
```

### 4.2 AML Screening Pipeline

```
Entity for Screening
        │
        ▼
┌───────────────────┐
│ Sanctions Check   │──▶ Aerospike + Sumsub
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ Velocity Check    │──▶ Redis Statistics
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ Pattern Detection │──▶ Rule Engine
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│ Risk Score        │
│ Aggregation       │
└────────┬──────────┘
         │
         ▼
    Case/Alert Creation
```

---

## 5. Integration Points

### 5.1 External System Integrations

| System | Protocol | Purpose |
|--------|----------|---------|
| ML Scoring Service | REST/HTTP | XGBoost model inference |
| Sumsub | REST/HTTPS | KYC/AML screening |
| VGS (Optional) | HTTPS Proxy | PCI-DSS tokenization |
| Slack/Email | Webhooks | Notifications |

### 5.2 API Integration Pattern

```java
// RestTemplate with Circuit Breaker
@Autowired
private RestClientService restClient;

@CircuitBreaker(name = "scoring")
@Retry(name = "scoring")
public ScoringResponse callScoringService(ScoringRequest request) {
    return restClient.post(scoringUrl, request, ScoringResponse.class);
}
```

---

## 6. Security Architecture

### 6.1 Authentication Flow

```
User Request
     │
     ▼
┌────────────────────┐
│ Spring Security    │
│ Filter Chain       │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐     ┌───────────────────┐
│ Form Login         │────▶│ User Details      │
│ Authentication     │     │ Service           │
└────────┬───────────┘     └───────────────────┘
         │
         ▼
┌────────────────────┐
│ Session Management │──▶ Concurrent Session Control
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ Permission Check   │──▶ Role-Based Access Control
└────────────────────┘
```

### 6.2 Multi-Tenant PSP Data Isolation

The system implements comprehensive PSP (Payment Service Provider) data isolation to ensure multi-tenant security:

```
┌─────────────────────────────────────────────────────────────┐
│                    PSP Isolation Architecture                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌──────────────────────────┐     │
│  │ PSP User     │────────▶│ PspIsolationService      │     │
│  │ (PSP_ADMIN)  │         │ • getCurrentUserPspId()  │     │
│  └──────────────┘         │ • validatePspAccess()    │     │
│                           │ • sanitizePspId()        │     │
│  ┌──────────────┐         │ • isPlatformAdmin()     │     │
│  │ Platform     │────────▶│                         │     │
│  │ Admin        │         └───────────┬──────────────┘     │
│  │ (ADMIN)      │                     │                    │
│  └──────────────┘                     ▼                    │
│                           ┌──────────────────────────┐     │
│                           │ Controllers & Services   │     │
│                           │ • TransactionController  │     │
│                           │ • MerchantController     │     │
│                           │ • ComplianceCaseCtrl    │     │
│                           └───────────┬──────────────┘     │
│                                       │                    │
│                                       ▼                    │
│                           ┌──────────────────────────┐     │
│                           │ Repositories             │     │
│                           │ • findByPspId()         │     │
│                           │ • countByPspId()         │     │
│                           └──────────────────────────┘     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

**Key Components:**
- **PspIsolationService**: Centralized service enforcing PSP data isolation
- **CasePermissionService**: PSP-aware case access control
- **Controller-Level Filtering**: All controllers filter by PSP ID
- **Repository-Level Queries**: PSP-specific query methods
- **Automatic PSP ID Sanitization**: Prevents PSP users from overriding PSP ID parameter

**Security Features:**
- ✅ PSP users can only access their own PSP's data
- ✅ Platform Administrators can access all PSPs
- ✅ PSP ID parameter sanitization prevents override
- ✅ Defense-in-depth: Controller, service, and repository layers
- ✅ Security violations logged for audit trail
- ✅ Fail-safe defaults: Deny access if PSP not assigned

### 6.3 Security Controls

| Control | Implementation |
|---------|----------------|
| **PSP Data Isolation** | `PspIsolationService` with automatic filtering |
| **Role-Based Access** | Dynamic RBAC with granular permissions |
| **Authentication** | Spring Security form-based login |
| **Session Management** | Concurrent session control, timeout |
| **Audit Logging** | Immutable audit trail with HMAC |
| **Input Validation** | Jakarta Validation annotations |
| **SQL Injection Prevention** | JPA/Hibernate parameterized queries |
| **XSS Prevention** | Content Security Policy headers |
| Password Hashing | BCrypt |
| PAN Tokenization | SHA-256 |
| Session Management | Spring Security + Redis |
| CSRF Protection | Enabled |
| XSS Prevention | OWASP Encoder |

---

## 7. Performance Characteristics

### 7.1 Throughput Metrics

| Metric | Value |
|--------|-------|
| Max Concurrent Requests | 30,000+ |
| Requests per Second | 50,000+ |
| Transaction Processing Latency | < 200ms |
| Database Connection Pool | 300 connections |
| HTTP Connection Pool | 30,000 connections |

### 7.2 Optimization Strategies

- **Async Processing**: 50-200 thread pool for non-blocking operations
- **Connection Pooling**: HikariCP for database, Apache HttpClient for HTTP
- **Caching**: Multi-tier (Aerospike + Redis + Caffeine)
- **Batch Processing**: 500 transactions per batch
- **Request Buffering**: 50,000 capacity queue

---

## 8. Deployment Architecture

### 8.1 Recommended Production Setup

```
                    ┌─────────────────┐
                    │  Load Balancer  │
                    │  (Nginx/HAProxy)│
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  App #1  │   │  App #2  │   │  App #3  │
        │  :2637   │   │  :2637   │   │  :2637   │
        └────┬─────┘   └────┬─────┘   └────┬─────┘
             │              │              │
             └──────────────┼──────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐
        │PostgreSQL│  │Aerospike │  │  Redis   │
        │ Primary  │  │ Cluster  │  │ Cluster  │
        └──────────┘  └──────────┘  └──────────┘
```

### 8.2 Port Configuration

| Service | Default Port |
|---------|-------------|
| Application | 2637 |
| PostgreSQL | 5432 |
| Aerospike | 3000 |
| Redis | 6379 |
| ML Scoring Service | 8000 |

---

## 9. Monitoring & Observability

### 9.1 Monitoring Stack

| Tool | Purpose |
|------|---------|
| Spring Actuator | Health checks, metrics |
| Prometheus | Metrics collection |
| Grafana | Dashboards |
| Application Logs | SLF4J + Logback |

### 9.2 Key Metrics

- Transaction processing latency
- ML model inference time
- Database query performance
- Cache hit/miss rates
- Error rates by type
- Active sessions count

---

## 10. Appendices

### A. Environment Variables

See [08-Deployment-Guide.md](08-Deployment-Guide.md) for complete list.

### B. Related Documents

- [02-Functional-Specification.md](02-Functional-Specification.md)
- [05-API-Reference.md](05-API-Reference.md)
- [06-Database-Design.md](06-Database-Design.md)
