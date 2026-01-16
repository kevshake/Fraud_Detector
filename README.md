# AML Fraud Detector System

> **Enterprise-grade Anti-Money Laundering and Fraud Detection Platform**

A comprehensive, multi-tenant AML/fraud detection system with real-time transaction monitoring, ML-powered risk scoring, case management, and regulatory compliance reporting.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Security & Multi-Tenancy](#security--multi-tenancy)
- [Development Guidelines](#development-guidelines)
- [Documentation](#documentation)
- [Support](#support)

---

## 🎯 Overview

The AML Fraud Detector is a production-ready platform designed for financial institutions, payment service providers (PSPs), and banks to detect and prevent money laundering and fraudulent transactions in real-time.

### What It Does

- **Real-time Transaction Monitoring**: Process and score transactions in milliseconds
- **ML-Powered Risk Scoring**: Multiple scoring systems (ML, KYC, TRS, CRA, Anomaly Detection)
- **Case Management**: Full workflow for investigating suspicious activities
- **Regulatory Compliance**: SAR filing, audit trails, and regulatory reporting
- **Multi-Tenant Architecture**: Complete PSP isolation with role-based access control
- **Sanctions Screening**: Real-time screening against sanctions lists
- **Graph Analytics**: Network analysis for detecting complex fraud patterns

### Who It's For

- **Payment Service Providers (PSPs)**: Monitor merchant transactions
- **Banks**: AML compliance and fraud prevention
- **Financial Institutions**: Regulatory compliance and risk management
- **Compliance Officers**: Case investigation and SAR filing
- **Analysts**: Transaction analysis and pattern detection

---

## ✨ Key Features

### 🔍 Transaction Processing
- Real-time fraud scoring with ML models (XGBoost)
- Multiple scoring systems working in concert
- Configurable decision thresholds (BLOCK, HOLD, ALERT, ALLOW)
- Sub-100ms processing latency
- Support for 30,000+ TPS (optimized configuration)

### 📊 Risk Scoring Systems
1. **ML Score**: XGBoost-based fraud prediction (0.0-1.0)
2. **KYC Risk Score (KRS)**: Customer/merchant profile risk (0-100)
3. **Transaction Risk Score (TRS)**: Transaction-specific risk (0-100)
4. **Customer Risk Assessment (CRA)**: Evolving customer risk (0-100)
5. **Anomaly Detection**: Autoencoder-based anomaly detection (0.0-1.0)
6. **Fraud Score**: Rule-based fraud points (0-100+)
7. **AML Score**: AML-specific risk points (0-100+)

### 🎫 Case Management
- Automated case creation from alerts and rules
- Workflow management (NEW → ASSIGNED → INVESTIGATING → RESOLVED)
- SLA tracking and breach detection
- Case enrichment with transaction context
- Network graph visualization
- Timeline tracking
- Assignment and escalation workflows

### 🔐 Security & Compliance
- **Multi-tenant PSP isolation**: Complete data segregation
- **Role-based access control**: Fine-grained permissions
- **Session management**: 30-minute timeout, session fixation protection
- **Audit trails**: 7-year retention for regulatory compliance
- **PII protection**: PAN hashing, field encryption
- **PSP-based logging**: MDC injection for log segregation

### 📈 Analytics & Reporting
- Real-time dashboards with Grafana
- Risk heatmaps (customer, merchant, geographic)
- Audit report generation
- User activity tracking
- Disposition statistics
- Revenue analytics

### 🌐 Sanctions Screening
- Real-time screening against OFAC, UN, EU sanctions lists
- Fuzzy matching for name variations
- Configurable match thresholds
- Automatic case creation on matches

---

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND LAYER                            │
│  React SPA (TypeScript) - Material-UI - TanStack Query          │
│  Port: 5173 (dev) / Static files (prod)                         │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        BACKEND LAYER                             │
│  Spring Boot 3.2.0 (Java 17) - Port: 2637                       │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Controllers → Services → Repositories                    │   │
│  │ • Transaction Processing  • Case Management              │   │
│  │ • Alert Management       • User Management               │   │
│  │ • Audit & Reporting      • Analytics                     │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  PostgreSQL  │    │  Aerospike   │    │    Neo4j     │
│  (Primary)   │    │  (Cache)     │    │   (Graph)    │
│              │    │              │    │              │
│ • Txns       │    │ • Stats      │    │ • Networks   │
│ • Cases      │    │ • Sanctions  │    │ • Patterns   │
│ • Users      │    │ • Velocity   │    │ • Anomalies  │
│ • Audit      │    │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
```

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  REST Controllers + Static Web Assets (HTML/JS)                 │
├─────────────────────────────────────────────────────────────────┤
│                     BUSINESS LOGIC LAYER                        │
│  Services + Orchestrators + Validators                          │
├─────────────────────────────────────────────────────────────────┤
│                     DATA ACCESS LAYER                           │
│  JPA Repositories + Aerospike Client + Neo4j Client             │
├─────────────────────────────────────────────────────────────────┤
│                     INFRASTRUCTURE LAYER                        │
│  PostgreSQL + Aerospike + Neo4j + Prometheus + Grafana          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Build Tool**: Maven 3.9+
- **Primary Database**: PostgreSQL 15+
- **Cache**: Aerospike 6.x
- **Graph Database**: Neo4j 5.x (optional)
- **Security**: Spring Security 6.x
- **Monitoring**: Prometheus + Grafana
- **ML**: XGBoost (via REST), DL4J (Anomaly Detection)

### Frontend
- **Framework**: React 18
- **Language**: TypeScript 5.2
- **Build Tool**: Vite 5.0
- **UI Library**: Material-UI 5.14
- **State Management**: TanStack Query 5.14
- **Charts**: Chart.js, Recharts
- **HTTP Client**: Axios

### Infrastructure
- **Containerization**: Docker (optional)
- **Reverse Proxy**: Nginx (production)
- **Monitoring**: Prometheus + Grafana
- **Logging**: SLF4J + Logback

---

## 📁 Project Structure

```
AML_FRAUD_DETECTOR/
│
├── BACKEND/                          # Spring Boot Backend
│   ├── src/main/java/com/posgateway/aml/
│   │   ├── config/                   # Configuration classes
│   │   │   ├── SecurityConfig.java   # Spring Security
│   │   │   ├── PspLoggingFilter.java # PSP-based logging (MDC)
│   │   │   └── ...
│   │   ├── controller/               # REST API endpoints
│   │   │   ├── AlertController.java
│   │   │   ├── ComplianceCaseController.java
│   │   │   ├── TransactionController.java
│   │   │   └── ...
│   │   ├── service/                  # Business logic
│   │   │   ├── risk/                 # Risk scoring services
│   │   │   ├── compliance/           # Compliance services
│   │   │   │   └── AuditReportService.java
│   │   │   ├── psp/                  # PSP services
│   │   │   │   └── RequestCounter.java
│   │   │   └── ...
│   │   ├── entity/                   # JPA entities
│   │   ├── repository/               # Data access
│   │   ├── dto/                      # Data transfer objects
│   │   └── exception/                # Custom exceptions
│   ├── src/main/resources/
│   │   ├── application.properties    # Main configuration
│   │   └── logback-spring.xml        # Logging configuration
│   └── pom.xml                       # Maven dependencies
│
├── FRONTEND/                         # React Frontend
│   ├── src/
│   │   ├── components/               # Reusable components
│   │   ├── pages/                    # Page components
│   │   ├── services/                 # API services
│   │   ├── hooks/                    # Custom React hooks
│   │   ├── types/                    # TypeScript types
│   │   └── App.tsx                   # Main app component
│   ├── public/                       # Static assets
│   ├── package.json                  # NPM dependencies
│   └── vite.config.ts                # Vite configuration
│
└── docs/                             # Documentation
    ├── 01-Technical-Architecture.md  # Architecture details
    ├── 02-Functional-Specification.md
    ├── 03-Software-Requirements-Specification.md
    ├── 04-Software-Design-Document.md
    ├── 05-API-Reference.md           # Complete API docs
    ├── 06-Database-Design.md
    ├── 07-User-Guide.md
    ├── 08-Deployment-Guide.md
    ├── DEVELOPMENT_RULES.md          # Development standards
    ├── PROJECT_RULES.md              # Project-specific rules
    ├── DOCUMENTATION_QUICK_REFERENCE.md  # Doc guide
    └── ...
```

---

## 🚀 Getting Started

### Prerequisites

**Backend:**
- Java 17 or higher
- Maven 3.9+
- PostgreSQL 15+
- Aerospike 6.x (optional for full features)

**Frontend:**
- Node.js 18+ and npm 9+

### Installation

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd AML_FRAUD_DETECTOR
```

#### 2. Database Setup

**PostgreSQL:**
```bash
# Create database
createdb aml_fraud_detector

# Database will be auto-initialized by Spring Boot
```

**Aerospike** (Optional):
```bash
# Install and start Aerospike
# See docs/AEROSPIKE_CONNECTION_SETUP.md
```

#### 3. Backend Setup
```bash
cd BACKEND

# Configure database connection
# Edit src/main/resources/application.properties
# Set: spring.datasource.url, username, password

# Build and run
mvn clean install
mvn spring-boot:run

# Backend will start on http://localhost:2637
```

#### 4. Frontend Setup
```bash
cd FRONTEND

# Install dependencies
npm install

# Start development server
npm run dev

# Frontend will start on http://localhost:5173
```

#### 5. Access the Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:2637/api/v1
- **Swagger UI**: http://localhost:2637/swagger-ui.html
- **Actuator Health**: http://localhost:2637/actuator/health

### Default Credentials

```
Super Admin:
  Email: super.admin@aml.com
  Password: Admin@123

PSP Admin:
  Email: psp.admin@bank.com
  Password: Admin@123
```

---

## ⚙️ Configuration

### Backend Configuration

Key configuration in `BACKEND/src/main/resources/application.properties`:

```properties
# Server
server.port=2637
server.servlet.context-path=/

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/aml_fraud_detector
spring.datasource.username=postgres
spring.datasource.password=your_password

# Session Management
server.servlet.session.timeout=30m

# Aerospike (optional)
aerospike.enabled=true
aerospike.hosts=localhost:3000

# ML Scoring
ml.scoring.url=http://localhost:8000/score
ml.scoring.enabled=true

# Thresholds
fraud.threshold.block=0.90
fraud.threshold.hold=0.70
fraud.threshold.alert=0.50
```

### Frontend Configuration

Configuration in `FRONTEND/vite.config.ts`:

```typescript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api/v1': {
        target: 'http://localhost:2637',
        changeOrigin: true
      }
    }
  }
})
```

### Environment Variables

See `docs/ENV_VARIABLES.md` for complete list of environment variables.

---

## 📚 API Documentation

### Base URL
```
http://localhost:2637/api/v1
```

### Authentication
All endpoints (except login) require session-based authentication.

```bash
# Login
POST /api/v1/auth/login
{
  "username": "super.admin@aml.com",
  "password": "Admin@123"
}

# Get current user
GET /api/v1/auth/me
```

### Key Endpoints

#### Transaction APIs
```bash
POST   /api/v1/transactions/ingest      # Ingest transaction
GET    /api/v1/transactions              # List transactions
GET    /api/v1/transactions/{id}         # Get transaction details
```

#### Alert APIs
```bash
GET    /api/v1/alerts                    # List alerts
GET    /api/v1/alerts/{id}               # Get alert details
PUT    /api/v1/alerts/{id}/resolve       # Resolve alert
DELETE /api/v1/alerts/{id}               # Delete alert
GET    /api/v1/alerts/count/active       # Get active count
GET    /api/v1/alerts/disposition-stats  # Get statistics
```

#### Case Management APIs
```bash
GET    /api/v1/compliance/cases          # List cases
GET    /api/v1/compliance/cases/{id}     # Get case details
GET    /api/v1/compliance/cases/stats    # Get statistics
DELETE /api/v1/compliance/cases/{id}     # Delete case
```

#### Audit & Reporting APIs
```bash
POST   /api/v1/audit/reports/generate           # Generate audit report
POST   /api/v1/audit/reports/user-activity      # User activity report
GET    /api/v1/audit/trail                      # Get audit trail
```

### Complete API Documentation

See **[docs/05-API-Reference.md](docs/05-API-Reference.md)** for complete API documentation with:
- Request/response examples
- Query parameters
- Error responses
- PSP filtering behavior
- Authentication requirements

### Interactive API Documentation

Access Swagger UI at: http://localhost:2637/swagger-ui.html

---

## 🔐 Security & Multi-Tenancy

### PSP Isolation

The system implements **strict PSP (Payment Service Provider) isolation**:

- **Super Admin (PSP ID 0)**: Can view all data across all PSPs
- **PSP Users**: Can only view/modify data for their assigned PSP
- **Automatic Filtering**: All queries automatically filter by PSP ID
- **Access Validation**: 403 Forbidden returned for cross-PSP access attempts

### Role-Based Access Control

**Roles:**
- `SUPER_ADMIN`: Platform administrator, full access
- `PSP_ADMIN`: PSP administrator, manage PSP users and settings
- `PSP_ANALYST`: Analyst, investigate cases and alerts
- `PSP_COMPLIANCE_OFFICER`: Compliance officer, file SARs

**Permissions:**
- `VIEW_ALL`: View all data (Super Admin only)
- `MANAGE_USERS`: User management
- `MANAGE_CASES`: Case management
- `VIEW_REPORTS`: Access reports
- `FILE_SAR`: File suspicious activity reports

### Security Features

- **Session Management**: 30-minute timeout, session fixation protection
- **Authentication**: Session-based with CSRF protection
- **Authorization**: Method-level security with `@PreAuthorize`
- **Audit Trails**: All actions logged with user, timestamp, IP
- **PII Protection**: PAN hashing (SHA-256), field encryption
- **PSP Logging**: MDC injection for log segregation
- **Rate Limiting**: Per-PSP request throttling

### PSP Logging Filter

The `PspLoggingFilter` automatically injects PSP context into all logs:

```
2026-01-16 09:00:00 [http-nio-8080-exec-1] INFO [PSP:PSP_BANK_A] - Fetching alerts
```

This enables:
- Log clustering by PSP
- PSP-specific audit trails
- Security incident investigation
- Compliance reporting

---

## 👨‍💻 Development Guidelines

### Documentation Rules

> **CRITICAL**: All API changes MUST be documented in `docs/05-API-Reference.md` immediately.

See **[docs/DEVELOPMENT_RULES.md](docs/DEVELOPMENT_RULES.md)** for:
- API documentation requirements
- Frontend-backend integration rules
- Data isolation requirements
- Recursive impact analysis
- Database configuration rules

See **[docs/PROJECT_RULES.md](docs/PROJECT_RULES.md)** for:
- Documentation standards
- Code organization
- Security standards
- Testing requirements
- Performance standards

### Quick Documentation Guide

Use **[docs/DOCUMENTATION_QUICK_REFERENCE.md](docs/DOCUMENTATION_QUICK_REFERENCE.md)** to quickly determine:
- Which `.md` file to update
- What to document
- Templates to use
- Common mistakes to avoid

### Code Standards

**Naming Conventions:**
- Controllers: `*Controller` (e.g., `AlertController`)
- Services: `*Service` (e.g., `AuditReportService`)
- Filters: `*Filter` (e.g., `PspLoggingFilter`)
- DTOs: `*Request`, `*Response`, `*DTO`

**Testing Requirements:**
- Unit tests: Minimum 80% coverage
- Integration tests: All API endpoints
- Test scenarios: Happy path, validation, auth, authorization, not found

**PSP Isolation:**
```java
// Always validate PSP access
if (!pspIsolationService.isPlatformAdministrator()) {
    Long userPspId = pspIsolationService.getCurrentUserPspId();
    if (!entity.getPspId().equals(userPspId)) {
        throw new ForbiddenException("Access denied");
    }
}
```

### Recursive Impact Analysis

> **CRITICAL**: Always check all affected classes after editing a class.

When modifying a class:
1. Identify all classes that depend on it
2. For each affected class, check its dependents
3. Continue recursively until no new affected classes
4. Review and update all identified classes
5. Compile and run tests

---

## 📖 Documentation

### Core Documentation

| Document | Description |
|----------|-------------|
| [01-Technical-Architecture.md](docs/01-Technical-Architecture.md) | Complete system architecture |
| [02-Functional-Specification.md](docs/02-Functional-Specification.md) | Feature specifications |
| [03-Software-Requirements-Specification.md](docs/03-Software-Requirements-Specification.md) | System requirements |
| [04-Software-Design-Document.md](docs/04-Software-Design-Document.md) | Component design details |
| [05-API-Reference.md](docs/05-API-Reference.md) | **Complete API documentation** |
| [06-Database-Design.md](docs/06-Database-Design.md) | Database schema and design |
| [07-User-Guide.md](docs/07-User-Guide.md) | End-user guide |
| [08-Deployment-Guide.md](docs/08-Deployment-Guide.md) | Deployment instructions |

### Development Documentation

| Document | Description |
|----------|-------------|
| [DEVELOPMENT_RULES.md](docs/DEVELOPMENT_RULES.md) | **Development standards** |
| [PROJECT_RULES.md](docs/PROJECT_RULES.md) | **Project-specific rules** |
| [DOCUMENTATION_QUICK_REFERENCE.md](docs/DOCUMENTATION_QUICK_REFERENCE.md) | **Quick doc guide** |
| [SCORING_PROCESS_DOCUMENTATION.md](docs/SCORING_PROCESS_DOCUMENTATION.md) | Scoring formulas |
| [PSP_ISOLATION_SECURITY_AUDIT.md](docs/PSP_ISOLATION_SECURITY_AUDIT.md) | PSP security audit |

### Operational Documentation

| Document | Description |
|----------|-------------|
| [GRAFANA_DASHBOARD_ACCESS_GUIDE.md](docs/GRAFANA_DASHBOARD_ACCESS_GUIDE.md) | Grafana setup |
| [PROMETHEUS_GRAFANA_SETUP.md](docs/PROMETHEUS_GRAFANA_SETUP.md) | Monitoring setup |
| [AEROSPIKE_CONNECTION_SETUP.md](docs/AEROSPIKE_CONNECTION_SETUP.md) | Aerospike setup |
| [ENV_VARIABLES.md](docs/ENV_VARIABLES.md) | Environment variables |

### Recent Updates

| Document | Description |
|----------|-------------|
| [DOCUMENTATION_UPDATE_2026-01-16.md](docs/DOCUMENTATION_UPDATE_2026-01-16.md) | Latest doc updates |

---

## 🧪 Testing

### Running Tests

**Backend:**
```bash
cd BACKEND

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AlertControllerTest

# Run with coverage
mvn clean test jacoco:report
```

**Frontend:**
```bash
cd FRONTEND

# Run tests
npm test

# Run with coverage
npm run test:coverage
```

### Test Coverage Requirements

- **Unit Tests**: Minimum 80% coverage
- **Integration Tests**: All API endpoints
- **Test Scenarios**: Happy path, validation errors, auth failures, authorization failures, not found

---

## 📊 Monitoring

### Prometheus Metrics

Access metrics at: http://localhost:2637/actuator/prometheus

**Key Metrics:**
- `http_server_requests_seconds` - Request latency
- `jvm_memory_used_bytes` - Memory usage
- `jdbc_connections_active` - Database connections
- Custom business metrics (alert resolution time, case processing time)

### Grafana Dashboards

Access Grafana at: http://localhost:3000

**Dashboards:**
- System Health
- Transaction Processing
- Alert Management
- Case Management
- PSP-specific metrics

See [docs/GRAFANA_DASHBOARD_ACCESS_GUIDE.md](docs/GRAFANA_DASHBOARD_ACCESS_GUIDE.md) for setup.

---

## 🐛 Troubleshooting

### Common Issues

**Backend won't start:**
- Check PostgreSQL is running
- Verify database credentials in `application.properties`
- Check port 2637 is not in use

**Frontend can't connect to backend:**
- Verify backend is running on port 2637
- Check Vite proxy configuration
- Check browser console for CORS errors

**Authentication issues:**
- Clear browser cookies
- Check session timeout (30 minutes)
- Verify user credentials

**PSP isolation not working:**
- Check user's PSP ID assignment
- Verify `PspLoggingFilter` is active
- Check logs for PSP context

### Logs

**Backend logs:**
```bash
# View logs
tail -f BACKEND/logs/application.log

# Logs include PSP context
[PSP:PSP_BANK_A] - Log message
```

**Frontend logs:**
- Check browser console (F12)
- Network tab for API calls

---

## 🤝 Contributing

### Before Submitting

Use the review checklist from [docs/PROJECT_RULES.md](docs/PROJECT_RULES.md):

- [ ] All new APIs documented in `05-API-Reference.md`
- [ ] All new components documented in `04-Software-Design-Document.md`
- [ ] PSP isolation implemented and tested
- [ ] Unit tests written with >80% coverage
- [ ] Integration tests written for all endpoints
- [ ] Error handling implemented
- [ ] Audit logging added for state changes
- [ ] Input validation implemented
- [ ] Code follows naming conventions
- [ ] No hardcoded values
- [ ] Recursive impact analysis performed

### Commit Messages

Format:
```
[Component] Brief description

Detailed explanation
- Change 1
- Change 2

Fixes #issue-number
```

---

## 📞 Support

### Documentation

- **Quick Start**: This README
- **API Reference**: [docs/05-API-Reference.md](docs/05-API-Reference.md)
- **Development Guide**: [docs/DEVELOPMENT_RULES.md](docs/DEVELOPMENT_RULES.md)
- **User Guide**: [docs/07-User-Guide.md](docs/07-User-Guide.md)

### Resources

- **Swagger UI**: http://localhost:2637/swagger-ui.html
- **Actuator**: http://localhost:2637/actuator
- **Grafana**: http://localhost:3000

---

## 📄 License

Proprietary - All rights reserved

---

## 🎯 Quick Links

- [Complete API Documentation](docs/05-API-Reference.md)
- [Development Rules](docs/DEVELOPMENT_RULES.md)
- [Project Rules](docs/PROJECT_RULES.md)
- [Documentation Quick Reference](docs/DOCUMENTATION_QUICK_REFERENCE.md)
- [Architecture Overview](docs/01-Technical-Architecture.md)
- [Deployment Guide](docs/08-Deployment-Guide.md)

---

**Version:** 1.0  
**Last Updated:** 2026-01-16  
**Status:** Production Ready

---

*For detailed information on any topic, please refer to the comprehensive documentation in the `docs/` directory.*
