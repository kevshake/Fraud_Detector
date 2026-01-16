# AML Fraud Detector - Architecture Overview

**Version:** 2.0 (Post-Restructuring)  
**Date:** January 12, 2026  
**Status:** Production-Ready with Modern Frontend

---

## System Overview

The AML Fraud Detector is an enterprise-grade Anti-Money Laundering and Fraud Detection system, now restructured as two independent projects for better scalability and maintainability.

---

## Project Structure

```
AML_FRAUD_DETECTOR/
│
├── BACKEND/                    # Spring Boot REST API
│   ├── src/main/java/         # Java source code (762 files)
│   ├── src/main/resources/    # Config, DB migrations
│   ├── pom.xml                 # Maven configuration
│   └── target/                 # Build output
│
├── FRONTEND/                   # React + TypeScript SPA
│   ├── src/                    # React components
│   ├── public/legacy_ui/       # Original UI (preserved)
│   ├── package.json            # npm dependencies
│   ├── vite.config.ts          # Vite configuration
│   └── tsconfig.json           # TypeScript config
│
└── docs/                       # Documentation
    ├── 01-Technical-Architecture.md
    ├── FINAL_STRUCTURE.md
    └── ... (90+ documents)
```

---

## Technology Stack

### Backend (BACKEND/)
| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17+ |
| Framework | Spring Boot | 3.2.0 |
| Security | Spring Security | 6.x |
| ORM | Hibernate/JPA | 6.x |
| Build | Maven | 3.6+ |
| **Port** | **2637** | |
| **API Base** | **/api/v1** | |

### Frontend (FRONTEND/)
| Component | Technology | Version |
|-----------|------------|---------|
| Framework | React | 18.2+ |
| Language | TypeScript | 5.2+ |
| Build Tool | Vite | 5.0+ |
| UI Library | Material-UI | 5.14+ |
| Data Fetching | TanStack Query | 5.14+ |
| Routing | React Router | 6.20+ |
| **Port** | **5173** (dev) | |

### Data Storage
| Database | Purpose |
|----------|---------|
| PostgreSQL 13+ | Primary RDBMS (users, cases, alerts, audit) |
| Aerospike 6.0+ | Transactions (primary), sanctions, caching |
| Redis | Session cache, statistics |

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         FRONTEND                             │
│                  React + TypeScript SPA                      │
│                   http://localhost:5173                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ REST API Calls
                         │ /api/v1/**
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                         BACKEND                              │
│                  Spring Boot REST API                        │
│                   http://localhost:2637                      │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Controllers (51 REST endpoints)                      │  │
│  │  • /api/v1/transactions                               │  │
│  │  • /api/v1/compliance/cases                           │  │
│  │  • /api/v1/merchants                                  │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Services (135 business logic services)              │  │
│  │  • Fraud Detection  • AML Screening                  │  │
│  │  • Case Management  • Reporting                      │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Repositories (61 data access layers)                │  │
│  └────────────────────┬─────────────────────────────────┘  │
└───────────────────────┼──────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
  ┌──────────┐    ┌──────────┐    ┌──────────┐
  │PostgreSQL│    │Aerospike │    │  Redis   │
  └──────────┘    └──────────┘    └──────────┘
```

---

## Communication Flow

### Frontend → Backend
1. **User Action** (e.g., view cases)
2. **React Component** calls API service
3. **TanStack Query** manages request/caching
4. **Vite Proxy** forwards `/api/v1/**` to backend
5. **Backend** processes request
6. **Response** returned as JSON
7. **React** updates UI

### Example API Call
```typescript
// Frontend (FRONTEND/src/services/casesApi.ts)
export const fetchCases = async () => {
  const response = await fetch('/api/v1/compliance/cases');
  return response.json();
};

// Backend handles at:
// BACKEND/src/main/java/com/posgateway/aml/controller/ComplianceCaseController.java
@GetMapping("/api/v1/compliance/cases")
public ResponseEntity<List<CaseDTO>> getCases() { ... }
```

---

## Deployment Model

### Development
```bash
# Terminal 1: Start Backend
cd BACKEND
mvn spring-boot:run
# → http://localhost:2637

# Terminal 2: Start Frontend
cd FRONTEND
npm install
npm run dev
# → http://localhost:5173
```

### Production
**Option A: Separate Deployments (Recommended)**
- Backend → Java hosting (AWS EC2, Azure App Service, etc.)
- Frontend → Static hosting (Vercel, Netlify, S3 + CloudFront, etc.)
- Frontend calls backend via public API URL

**Option B: Bundled Deployment**
- Build frontend: `npm run build` → `dist/`
- Copy `dist/` to `BACKEND/src/main/resources/static/`
- Deploy single Spring Boot JAR

---

## Key Features

### Security
- ✅ PSP (Payment Service Provider) data isolation
- ✅ Role-based access control (RBAC)
- ✅ Spring Security authentication
- ✅ CORS configured for frontend origin
- ✅ Immutable audit logging

### Performance
- ✅ 30,000+ concurrent requests
- ✅ <200ms transaction processing
- ✅ Aerospike primary storage (<1ms latency)
- ✅ Multi-tier caching (Aerospike + Redis + Caffeine)

### Compliance
- ✅ Real-time sanctions screening
- ✅ AML/CFT monitoring
- ✅ SAR/IFTR reporting
- ✅ Case management workflow
- ✅ Regulatory audit trail

---

## Integration Points

| System | Protocol | Purpose |
|--------|----------|---------|
| ML Scoring Service | REST/HTTP | XGBoost model inference |
| Sumsub | REST/HTTPS | KYC/AML screening |
| VGS (Optional) | HTTPS Proxy | PCI-DSS tokenization |
| Slack/Email | Webhooks | Notifications |

---

## Monitoring & Observability

| Tool | Purpose |
|------|---------|
| Spring Actuator | Health checks, metrics |
| Prometheus | Metrics collection |
| Grafana | Dashboards |
| Logback | Application logging |

---

## Migration Status

### ✅ Completed
- Backend fully functional with all 762 Java files
- React project initialized with TypeScript + Vite
- Original UI preserved in `FRONTEND/public/legacy_ui/`
- Documentation updated
- CORS configured

### 🔄 In Progress
- Migrating UI components from legacy HTML/JS to React
- Implementing Material-UI design system
- Setting up TanStack Query for data fetching

### 📋 Planned
- Complete dashboard migration
- Implement all case management views
- Add real-time updates with WebSockets
- Production deployment configuration

---

## Quick Reference

### Backend API Endpoints
- `GET /api/v1/compliance/cases` - List all cases
- `GET /api/v1/compliance/cases/{id}` - Get case details
- `POST /api/v1/compliance/cases/workflow/status` - Update case status
- `POST /api/v1/compliance/cases/workflow/escalate` - Escalate case
- `GET /api/v1/compliance/cases/stats` - Case statistics

### Frontend Routes (Planned)
- `/` - Dashboard
- `/cases` - Case management
- `/alerts` - Alert management
- `/merchants` - Merchant directory
- `/reports` - Compliance reports

---

## Documentation

### Key Documents
- `docs/01-Technical-Architecture.md` - Complete technical architecture
- `docs/FINAL_STRUCTURE.md` - Project structure overview
- `docs/frontend_migration_plan.md` - Frontend migration guide
- `FRONTEND/README.md` - Frontend setup and development
- `FRONTEND/SETUP.md` - Quick start guide

### API Documentation
- `docs/05-API-Reference.md` - Complete API reference
- Swagger UI: `http://localhost:2637/swagger-ui.html` (if enabled)

---

## Support & Maintenance

### Prerequisites
- **Backend:** Java 17+, Maven 3.6+
- **Frontend:** Node.js 20+, npm 10+
- **Databases:** PostgreSQL 13+, Aerospike 6.0+, Redis

### Getting Help
1. Check `docs/` folder for detailed documentation
2. Review `FRONTEND/README.md` for frontend-specific issues
3. Check `docs/TROUBLESHOOTING.md` (if available)

---

*Last Updated: January 12, 2026*  
*Architecture Version: 2.0 (Post-Restructuring)*
